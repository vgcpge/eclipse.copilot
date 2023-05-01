package org.vgcpge.copilot.ls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.vgcpge.copilot.ls.rpc.CopilotLanguageServer;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		InputStream input = System.in;
		PrintStream out = System.out;
		ExecutorService executorService = Executors.newCachedThreadPool();
		try {
			startProxy(input, out, System.err, executorService).get();
		} finally {
			executorService.shutdown();
		}
	}

	public static Future<Void> startProxy(InputStream input, OutputStream output, OutputStream error,
			ExecutorService executorService) throws IOException {
		Process copilotProcess = new ProcessBuilder(org.vgcpge.copilot.ls.CopilotLocator.copilotStartCommand())
				.redirectError(Redirect.INHERIT).start();
		CompletableFuture<LanguageClient> upstreamClient = new CompletableFuture<>();
		CompletableFuture<Void> serverIsInitialized = new CompletableFuture<Void>();

		LanguageClientDecorator client = new LanguageClientDecorator(upstreamClient, serverIsInitialized);

		Launcher<CopilotLanguageServer> clientLauncher = new Builder<CopilotLanguageServer>()
				.setExecutorService(executorService).setLocalService(client)
				.setRemoteInterface(CopilotLanguageServer.class).setInput(copilotProcess.getInputStream())
				.setOutput(copilotProcess.getOutputStream()).create();
		LanguageServerDecorator server = new LanguageServerDecorator(
				CompletableFuture.completedFuture(clientLauncher.getRemoteProxy()));

		Launcher<LanguageClient> launcher = new Builder<LanguageClient>().setExecutorService(executorService)
				.setLocalService((LanguageServer) server).setRemoteInterface(LanguageClient.class).setInput(input)
				.setOutput(output).create();
		upstreamClient.complete(launcher.getRemoteProxy());

		server.getInitialized().thenRunAsync(() -> {
			new Authentication(launcher.getRemoteProxy(), clientLauncher.getRemoteProxy());
		}, executorService);

		server.getInitialized().whenCompleteAsync((ignored, error2) -> {
			if (error2 == null) {
				serverIsInitialized.completeExceptionally(error2);
			} else {
				serverIsInitialized.complete(null);
			}
		}, executorService);

		launcher.startListening();

		return clientLauncher.startListening();

	}
}
