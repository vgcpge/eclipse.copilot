package org.vgcpge.eclipse.copilot.ui.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.vgcpge.copilot.ls.FinalCloser;
import org.vgcpge.copilot.ls.LanguageServer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class GithubCopilotProvider implements StreamConnectionProvider {
	private final FinalCloser closer = new FinalCloser();
	private final ExecutorService executorService;
	private final PipedInputStream input;
	private final PipedOutputStream output = new PipedOutputStream();
	{
		try {
			input = closer.register(new OrphanPipedInputStream());
			executorService = Executors
					.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("CopilotProxy-%d").build());
			closer.register(this::shutdownAndAwaitTermination);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void start() throws IOException {
		closer.register(new LanguageServer(closer.register(new OrphanPipedInputStream(output)),
				closer.register(new PipedOutputStream(input)),
				executorService));
		closer.register(output);
	}

	@Override
	public InputStream getInputStream() {
		return input;
	}

	@Override
	public OutputStream getOutputStream() {
		return output;
	}

	@Override
	public @Nullable InputStream getErrorStream() {
		// LSP4E never poll the return value, so we can't return anything here - unpolled error stream will lead to agent's hangup
		return null;
	}

	@Override
	public void stop() {
		try {
			closer.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void shutdownAndAwaitTermination() throws IOException {
		var pool = executorService;
		pool.shutdown();
		
		// Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					throw new IOException("Pool did not terminate");
			}
		} catch (InterruptedException ex) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
}
