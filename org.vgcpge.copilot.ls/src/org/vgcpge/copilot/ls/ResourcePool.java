package org.vgcpge.copilot.ls;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Listen for all resources to be closed */
final class ResourcePool {
	private long referenceCount = 0;
	private boolean closed;
	private final Object lock = new Object();
	private final Closeable onClose;

	public ResourcePool(Closeable onClose) {
		this.onClose = Objects.requireNonNull(onClose);
	}

	public Closeable allocate() throws ResourceClosedException {
		return new Handle();
	}

	private final class Handle implements Closeable {
		private final AtomicBoolean closed = new AtomicBoolean(false);

		public Handle() throws ResourceClosedException {
			synchronized (lock) {
				if (ResourcePool.this.closed) {
					throw new ResourceClosedException();
				}
				referenceCount++;
			}
		}

		@Override
		public void close() throws IOException {
			if (closed.compareAndSet(false, true)) {
				synchronized (lock) {
					referenceCount--;
					if (referenceCount == 0 && ResourcePool.this.closed) {
						onClose.close();
					}
				}
			}
		}
	}

}
