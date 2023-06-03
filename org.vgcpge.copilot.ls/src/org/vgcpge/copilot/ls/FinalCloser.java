package org.vgcpge.copilot.ls;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import com.google.common.base.Throwables;

/**
 * Thread-safe {@link com.google.common.io.Closer}
 * 
 * Closes all registered Closeables strictly in reverse order of registration.
 */
public final class FinalCloser implements Closeable {

	private final Deque<Closeable> stack = new ArrayDeque<>();
	private final Object lock = new Object();
	private boolean closed = false;
	/** Coordinates different closing threads, preventing out-of-order disposal. **/
	private boolean busy = false;
	private Throwable error = null;

	/**
	 * Registers the given {@code closeable} to be closed when this
	 * {@code FinalCloser} is {@linkplain #close closed}.
	 * 
	 * @throws IOException is this {@code FinalCloser} is already closed. Given
	 *                     {@code closeable} is immediately closed in this case.
	 *                     The close operation may be done in another thread.
	 * @return the given {@code closeable}
	 */
	public <T extends Closeable> T register(T closeable) throws IOException {
		if (closeable == null) // For compatibility with try-with-resources
			return closeable;
		boolean closedCopy;
		synchronized (lock) {
			stack.push(closeable);
			closedCopy = this.closed;
		}
		if (closedCopy) {
			IOException closedException = new ResourceClosedException();
			try {
				close();
			} catch (Exception e) {
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
	 * Close all registered objects. No futher operations are allowed. May be called
	 * twice.
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
					closeable = stack.pollFirst();
					if (closeable == null) {
						busy = false; // Can't do this in another synchronized section, as another thread may skip
										// execution while busy == true
						break;
					}
				}
				try {
					closeable.close();
				} catch (Exception e) {
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

	private void rememberError(Exception e) {
		assert e instanceof RuntimeException || e instanceof IOException;
		synchronized (lock) {
			if (error == null) {
				error = e;
			} else {
				error.addSuppressed(e);
			}
		}
	}

}
