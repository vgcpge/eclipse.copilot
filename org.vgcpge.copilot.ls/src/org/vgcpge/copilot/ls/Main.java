package org.vgcpge.copilot.ls;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		ExecutorService executorService = Executors.newCachedThreadPool();
		try (LanguageServer languageServer = new LanguageServer(System.in, System.out, executorService)) {
		} finally {
			executorService.shutdown();
		}
	}
}
