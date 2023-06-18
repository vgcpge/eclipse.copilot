package org.vgcpge.copilot.ls;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
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

	public LanguageServer(InputStream input, OutputStream output, ExecutorService executorService, Optional<ProxyConfiguration> proxyConfiguration) throws IOException {
		this.proxyConfiguration = Objects.requireNonNull(proxyConfiguration);
		try {
			startProxy(input, output, executorService);
		} catch (Throwable e) {
			closer.close();
			throw e;
		}
	}

	private void startProxy(InputStream input, OutputStream output, ExecutorService executorService)
			throws IOException {
		CompletableFuture<LanguageClient> upstreamClient = new CompletableFuture<>();
		CompletableFuture<Void> serverIsInitialized = new CompletableFuture<Void>();

		LanguageClientDecorator client = new LanguageClientDecorator(upstreamClient, serverIsInitialized);

		LanguageServerDecorator server = new LanguageServerDecorator(
				() -> startDownstreamServer(executorService, client));

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
		Process copilotProcess;
		CopilotLanguageServer downStreamServer;
		try {
			List<String> command = CopilotLocator.copilotStartCommand();
			String publicCommand = command.stream().map(CopilotLocator::privacyFilter).collect(Collectors.joining(" "));
			client.logMessage(new MessageParams(MessageType.Info, "Starting " + publicCommand));
			copilotProcess = new ProcessBuilder(command).redirectError(Redirect.INHERIT).start();
			register(copilotProcess::destroy);

			@SuppressWarnings("resource")
			OutputStream output = closer.register(copilotProcess.getOutputStream());
			@SuppressWarnings("resource")
			InputStream input = closer.register(copilotProcess.getInputStream());
			Launcher<CopilotLanguageServer> downstreamClientLauncher = new Builder<CopilotLanguageServer>()
					.setExecutorService(executorService).setLocalService(client)
					.setRemoteInterface(CopilotLanguageServer.class).setInput(input).wrapMessages(this::wrapMessages)
					.setOutput(output).create();
			downStreamServer = downstreamClientLauncher.getRemoteProxy();
			Future<Void> listenTask = downstreamClientLauncher.startListening();
			register(() -> listenTask.get());
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
