package org.vgcpge.copilot.ls;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.vgcpge.copilot.ls.rpc.CopilotLanguageServer;
import org.vgcpge.copilot.ls.rpc.EditorInfoParam;
import org.vgcpge.copilot.ls.rpc.EditorInfoParam.NetworkProxy;

import com.google.common.base.Throwables;

public class LanguageServer implements Closeable {
	private interface LongCloseable {
		void close() throws InterruptedException, IOException, ExecutionException, TimeoutException;
	}

	private final SafeCloser closer = new SafeCloser();
	private final Optional<ProxyConfiguration> proxyConfiguration;
	private final IOStreams downstream;

	public LanguageServer(IOStreams upstream, IOStreams downstream, ExecutorService executorService, Optional<ProxyConfiguration> proxyConfiguration) throws IOException {
		this.downstream = Objects.requireNonNull(downstream);
		this.proxyConfiguration = Objects.requireNonNull(proxyConfiguration);
		try {
			register(downstream);
			register(upstream);
			startProxy(upstream, executorService);
		} catch (Throwable e) {
			closer.close();
			throw e;
		}
	}

	private void startProxy(IOStreams upstream, ExecutorService executorService)
			throws IOException {
		CompletableFuture<LanguageClient> upstreamClient = new CompletableFuture<>();
		CompletableFuture<Void> serverIsInitialized = new CompletableFuture<Void>();

		LanguageClientDecorator client = new LanguageClientDecorator(upstreamClient, serverIsInitialized);

		LanguageServerDecorator server = new LanguageServerDecorator(
				() -> startDownstreamServer(executorService, client));

		@SuppressWarnings("resource")
		InputStream input = closer.register(upstream.input());
		@SuppressWarnings("resource")
		OutputStream output = closer.register(upstream.output());
		
		Launcher<LanguageClient> upstreamServerLauncher = new Builder<LanguageClient>()
				.setExecutorService(executorService).setLocalService(server).setRemoteInterface(LanguageClient.class)
				.setInput(input).setOutput(output).create();
		upstreamClient.complete(upstreamServerLauncher.getRemoteProxy());

		server.getInitialized().thenAcceptAsync((downStreamServer) -> {
			new Authentication(upstreamServerLauncher.getRemoteProxy(), downStreamServer);
		}, executorService);

		server.getInitialized()
		.thenCompose(this::configureProxy)
		.whenCompleteAsync((ignored, error2) -> {
			if (error2 == null) {
				serverIsInitialized.completeExceptionally(error2);
			} else {
				serverIsInitialized.complete(null);
			}
		}, executorService);

		Future<?> listenTask = upstreamServerLauncher.startListening();
		register(() -> listenTask.cancel(true));
	}
	
	private CompletableFuture<Void> configureProxy(CopilotLanguageServer server) {
		Optional<NetworkProxy> proxyOptional = proxyConfiguration.map(configuration -> {
			return new NetworkProxy(configuration.host(), configuration.port(), configuration.userId(), configuration.password(), configuration.rejectUnauthorized());
		});
		return server.setEditorInfo(new EditorInfoParam(proxyOptional));		
	}

	private CopilotLanguageServer startDownstreamServer(ExecutorService executorService,
			LanguageClientDecorator client) {
		CopilotLanguageServer downStreamServer;
		try {
			@SuppressWarnings("resource")
			OutputStream output = closer.register(downstream.output());
			@SuppressWarnings("resource")
			InputStream input = closer.register(downstream.input());
			Launcher<CopilotLanguageServer> downstreamClientLauncher = new Builder<CopilotLanguageServer>()
					.setExecutorService(executorService).setLocalService(client)
					.setRemoteInterface(CopilotLanguageServer.class).setInput(input).wrapMessages(this::wrapMessages)
					.setOutput(output).create();
			downStreamServer = downstreamClientLauncher.getRemoteProxy();
			Future<Void> listenTask = downstreamClientLauncher.startListening();
			register(() -> listenTask.get(2, TimeUnit.SECONDS));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return downStreamServer;
	}

	@SuppressWarnings("resource")
	private <T extends LongCloseable> T register(T closeable) throws IOException {
		closer.register(() -> {
			try {
				closeable.close();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			} catch (ExecutionException e) {
				if (e.getCause() != null) {
					Throwables.propagateIfPossible(e.getCause(), IOException.class);
				}
				throw new IOException(e);
			} catch (TimeoutException e) {
				throw new IOException(e);
			}
		});
		return closeable;
	}
	
	@SuppressWarnings("resource")
	private void register(IOStreams iostreams) throws IOException {
		closer.register(iostreams.input());
		closer.register(iostreams.output());
	}

	@Override
	public void close() throws IOException {
		closer.close();
	}

	private MessageConsumer wrapMessages(MessageConsumer messageconsumer1) {
		return message -> {
			if (message instanceof RequestMessage) {
				RequestMessage request = (RequestMessage) message;
				// Fix "Invalid params: must be object"
				if (request.getParams() == null) {
					request.setParams(new Object());
				}
			}
			messageconsumer1.consume(message);
		};
	}

}
