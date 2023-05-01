package org.vgcpge.eclipse.copilot.ui.internal;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/** Wait for more writers, instead of throwing exceptions **/
public class OrphanPipedInputStream extends PipedInputStream {
	public OrphanPipedInputStream(PipedOutputStream output) throws IOException {
		super(output);
	}

	public OrphanPipedInputStream() {
	}

	@Override
	public synchronized int read() throws IOException {
		for (;;) {
			try {
				return super.read();
			} catch (IOException e) {
				String message = e.getMessage();
				if ("Write end dead".equals(message) || "Pipe broken".equals(message)) {
					try {
						wait();
					} catch (InterruptedException e1) {
						Thread.currentThread().interrupt();
						throw new InterruptedIOException();
					}
					continue;
				}
				throw e;
			}
		}
	}
}
