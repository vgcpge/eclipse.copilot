package org.vgcpge.eclipse.copilot.ui.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

import com.google.common.io.Closer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class GithubCopilotProvider implements StreamConnectionProvider {
	private final Closer closer = Closer.create();
	private final ExecutorService executorService;
	{
		executorService = Executors
				.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("CopilotProxy-%d").build());
		closer.register(executorService::shutdownNow);
	}
	private final PipedInputStream input = closer.register(new OrphanPipedInputStream());
	private final PipedOutputStream output = new PipedOutputStream();
	private final PipedInputStream error = closer.register(new OrphanPipedInputStream());

	@Override
	public void start() throws IOException {
		org.vgcpge.copilot.ls.Main.startProxy(closer.register(new OrphanPipedInputStream(output)),
				closer.register(new PipedOutputStream(input)), closer.register(new PipedOutputStream(error)),
				executorService);
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
		return error;
	}

	@Override
	public void stop() {
		try {
			closer.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
