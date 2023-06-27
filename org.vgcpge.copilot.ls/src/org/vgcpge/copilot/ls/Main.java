package org.vgcpge.copilot.ls;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		ExecutorService executorService = Executors.newCachedThreadPool();
		var locator = new CopilotLocator(System.err::println);
		try (LanguageServer languageServer = new LanguageServer(new IOStreams(System.in, System.out), locator.start(),
				executorService, Optional.empty())) {
		} finally {
			executorService.shutdown();
		}
	}
}
