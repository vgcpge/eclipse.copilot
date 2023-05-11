package org.vgcpge.copilot.ls;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.vgcpge.copilot.ls.rpc.CopilotLanguageServer;

import com.google.common.io.Closer;

public class LanguageServer implements Closeable {
	private interface LongCloseable {
		void close() throws InterruptedException, IOException, ExecutionException;
	}

	private final Closer closer = Closer.create();
	
	public LanguageServer(InputStream input, OutputStream output, ExecutorService executorService) throws IOException {
		try {
			startProxy(input, output, executorService);
		} catch (Throwable e) {
			closer.close();
			throw e;
		}
	}

	private void startProxy(InputStream input, OutputStream output,
			ExecutorService executorService) throws IOException {
		CompletableFuture<LanguageClient> upstreamClient = new CompletableFuture<>();
		CompletableFuture<Void> serverIsInitialized = new CompletableFuture<Void>();

		LanguageClientDecorator client = new LanguageClientDecorator(upstreamClient, serverIsInitialized);

		LanguageServerDecorator server = new LanguageServerDecorator(() -> startDownstreamServer(executorService, client));

		Launcher<LanguageClient> upstreamServerLauncher = new Builder<LanguageClient>().setExecutorService(executorService)
				.setLocalService(server).setRemoteInterface(LanguageClient.class).setInput(input).setOutput(output)
				.create();
		upstreamClient.complete(upstreamServerLauncher.getRemoteProxy());

		server.getInitialized().thenAcceptAsync((downStreamServer) -> {
			new Authentication(upstreamServerLauncher.getRemoteProxy(), downStreamServer);
		}, executorService);

		server.getInitialized().whenCompleteAsync((ignored, error2) -> {
			if (error2 == null) {
				serverIsInitialized.completeExceptionally(error2);
			} else {
				serverIsInitialized.complete(null);
			}
		}, executorService);
		
		register(upstreamServerLauncher.startListening()::get);
	}
	
	private CopilotLanguageServer startDownstreamServer(ExecutorService executorService, LanguageClientDecorator client) {
		Process copilotProcess;
		try {
			copilotProcess = new ProcessBuilder(org.vgcpge.copilot.ls.CopilotLocator.copilotStartCommand())
					.redirectError(Redirect.INHERIT).start();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		register(copilotProcess::destroy);

		Launcher<CopilotLanguageServer> downstreamClientLauncher = new Builder<CopilotLanguageServer>()
				.setExecutorService(executorService).setLocalService(client)
				.setRemoteInterface(CopilotLanguageServer.class).setInput(copilotProcess.getInputStream())
				.wrapMessages(this::wrapMessages)
				.setOutput(copilotProcess.getOutputStream()).create();
		CopilotLanguageServer downStreamServer = downstreamClientLauncher.getRemoteProxy();
		register(downstreamClientLauncher.startListening()::get);
		register(downStreamServer::shutdown);
		return downStreamServer;
	}
	
	
	
	private synchronized <T extends LongCloseable> T register(T closeable) {
		closer.register(() -> {
			try {
				closeable.close();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}	catch (ExecutionException e) {
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
				// Fix "Invalid params:  must be object" 
				if (request.getParams() == null) {
					request.setParams(new Object());
				}
			}
			messageconsumer1.consume(message);
		};
	}


}
