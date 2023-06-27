package org.vgcpge.copilot.ls;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public final class IOStreams {
	private final InputStream input;
	private final OutputStream output;

	public IOStreams(InputStream input, OutputStream output) {
		super();
		this.input = Objects.requireNonNull(input);
		this.output = Objects.requireNonNull(output);
	}

	public InputStream input() {
		return input;
	}

	public OutputStream output() {
		return output;
	}

	/** Dispose resources when both streams are closed */
	public static IOStreams whenClosed(IOStreams streams, Closeable onClose) throws IOException {
		try {
			ResourcePool pool = new ResourcePool(onClose);
			@SuppressWarnings("resource")
			InputStream input = new FilterInputStream(streams.input()) {
				private final Closeable inputHandle = pool.allocate();

				@Override
				public void close() throws IOException {
					super.close();
					inputHandle.close();
				}
			};
			@SuppressWarnings("resource")
			OutputStream output = new FilterOutputStream(streams.output()) {
				private final Closeable outputHandle = pool.allocate();

				@Override
				public void close() throws IOException {
					super.close();
					outputHandle.close();
				}
			};
			return new IOStreams(input, output);
		} catch (Throwable e) {
			onClose.close();
			throw e;
		}
	}

}
