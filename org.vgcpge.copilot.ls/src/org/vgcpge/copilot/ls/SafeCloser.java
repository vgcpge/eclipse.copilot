package org.vgcpge.copilot.ls;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import com.google.common.base.Throwables;

/**
 * Closes all registered Closeables strictly in reverse order of registration.
 * 
 * Differences from  {@link com.google.common.io.Closer}:
 * <li> Closing of a resource may be done out of registration thread. All registered resources need to be thread-safe.
 * <li> Close operation is final, further registrations throw  {@link ResourceClosedException and dispose given resources ASAP. 
 * 
 * @see com.google.common.io.Closer
 */
public final class SafeCloser implements Closeable {

	private final Deque<Closeable> stack = new ArrayDeque<>();
	private final Object lock = new Object();
	private boolean closed = false;
	/** Coordinates different closing threads, preventing out-of-order disposal. **/
	private boolean busy = false;
	private Throwable error = null;

	/**
	 * @param closeable the {@code Closeable} to close when {@code FinalCloser} is
	 *                  {@linkplain #close closed}.
	 * @throws ResourceClosedException if this {@code FinalCloser} is already closed. Given
	 *                     {@code closeable} will be closed ASAP. The operation
	 *                     may be done in another thread.
	 * @throws IOException if any resources closed throw.
	 * @return the given {@code closeable}
	 */
	public <T extends Closeable> T register(T closeable) throws IOException {
		if (closeable == null) // For compatibility with try-with-resources
			return closeable;
		boolean closedCopy;
		synchronized (lock) {
			closedCopy = this.closed;
			stack.push(closeable);
		}
		if (closedCopy) {
			IOException closedException = new ResourceClosedException();
			try {
				close();
			} catch (Throwable e) {
				closedException.addSuppressed(e);
			}
			throw closedException;
		}
		return closeable;
	}

	public boolean isClosed() {
		synchronized (lock) {
			return closed;
		}
	}

	/**
	 * Close all registered objects. No further operations are allowed. All attempts
	 * to register more resources will result in them being closed ASAP. May be
	 * called multiple times.
	 **/
	@Override
	public void close() throws IOException {
		synchronized (lock) {
			closed = true;
			if (busy) {
				return;
			}
			busy = true;
		}
		try {
			for (;;) {
				Closeable closeable = null;
				synchronized (lock) {
					var tmp = stack.pollFirst();
					closeable = tmp; // Prevent false leaked resource warning
					if (closeable == null) {
						busy = false; // Another thread may skip execution while busy == true, reset flag now
						break;
					}
				}
				try {
					// External code may take a significant time to execute, so we are calling it
					// outside of mutex protection
					closeable.close();
				} catch (Throwable e) {
					rememberError(e);
				}
			}
		} finally {
			synchronized (lock) {
				if (error != null) {
					Throwables.propagateIfPossible(error, IOException.class);
					throw new AssertionError(error); // not possible, because error can only be either RuntimeException
														// or IOException
				}
			}
		}
	}

	private void rememberError(Throwable e) {
		assert e instanceof RuntimeException || e instanceof IOException || e instanceof Error;
		synchronized (lock) {
			if (error == null) {
				error = e;
			} else {
				error.addSuppressed(e);
			}
		}
	}

}
