package org.vgcpge.copilot.ls;

import java.io.Closeable;
import java.io.IOException;

import com.google.common.io.Closer;

/**
 * Closes all registered Closeables strictly in reverse order of registration.
 * 
 * Differences from {@link com.google.common.io.Closer}:
 * <li>Closing of a resource may be done out of registration thread. All
 * registered resources need to be thread-safe.
 * <li>Close operation is final, further registrations throw
 * {@link ResourceClosedException and dispose given resources ASAP.
 * 
 * @see com.google.common.io.Closer
 */
public final class SafeCloser implements Closeable {
	private final Closer closer = Closer.create();
	private final Object lock = new Object();
	private boolean closed = false;

	/**
	 * @param closeable the {@code Closeable} to close when {@code FinalCloser} is
	 *                  {@linkplain #close closed}.
	 * @throws ResourceClosedException if this {@code FinalCloser} is already
	 *                                 closed. Given {@code closeable} will be
	 *                                 closed ASAP. The operation may be done in
	 *                                 another thread.
	 * @throws IOException             if any resources closed throw.
	 * @return the given {@code closeable}
	 */
	public <T extends Closeable> T register(T closeable) throws IOException {
		// For compatibility with try-with-resources
		if (closeable == null)
			return closeable;
		boolean closedCopy;
		synchronized (lock) {
			closedCopy = this.closed;
			if (!closedCopy) {
				return closer.register(closeable);
			}
		}
		IOException closedException = new ResourceClosedException();
		try {
			closeable.close();
		} catch (Throwable e) {
			closedException.addSuppressed(e);
		}
		throw closedException;
	}

	public boolean isClosed() {
		synchronized (lock) {
			return closed;
		}
	}

	/**
	 * Close all registered objects. No further registrations are allowed. All
	 * attempts to register more resources will result in them being closed ASAP.
	 * May be called multiple times.
	 */
	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (closed) {
				return;
			}
			closed = true;
		}
		closer.close();
	}
}
