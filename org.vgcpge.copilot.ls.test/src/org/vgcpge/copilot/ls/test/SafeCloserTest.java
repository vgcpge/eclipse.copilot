package org.vgcpge.copilot.ls.test;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.vgcpge.copilot.ls.ResourceClosedException;
import org.vgcpge.copilot.ls.SafeCloser;

public class SafeCloserTest {

	@Test
	public void empty() throws IOException {
		try (SafeCloser closer = new SafeCloser()) {

		}
	}

	private static final class CloseableMock implements Closeable {
		public AtomicBoolean closed = new AtomicBoolean(false);

		@Override
		public void close() throws IOException {
			closed.set(true);
		}
	}

	@Test
	@SuppressWarnings("resource")
	public void multiple() throws IOException {
		CloseableMock[] mocks = new CloseableMock[] { new CloseableMock(), new CloseableMock(), new CloseableMock() };
		try (SafeCloser closer = new SafeCloser()) {
			for (CloseableMock mock : mocks) {
				Assert.assertSame(mock, closer.register(mock));
			}
			for (CloseableMock mock : mocks) {
				Assert.assertFalse(mock.closed.get());
			}
		}
		for (CloseableMock mock : mocks) {
			Assert.assertTrue(mock.closed.get());
		}
	}

	private static final class TestError extends Error {

		private static final long serialVersionUID = 330494317489527704L;

	}

	@SuppressWarnings("resource")
	@Test(expected = TestError.class)
	public void error() throws IOException {
		Closeable erroneous = () -> {
			throw new TestError();
		};
		CloseableMock first = new CloseableMock();
		CloseableMock last = new CloseableMock();
		Closeable[] mocks = new Closeable[] { first, erroneous, last };
		try (SafeCloser closer = new SafeCloser()) {
			for (Closeable mock : mocks) {
				Assert.assertSame(mock, closer.register(mock));
			}
			Assert.assertFalse(first.closed.get());
			Assert.assertFalse(last.closed.get());
		} finally {
			Assert.assertTrue(last.closed.get());
			Assert.assertTrue(first.closed.get());
		}
	}

	@SuppressWarnings("resource")
	@Test(timeout=15000)
	public void concurrentClosing() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Set<Closeable> resources = Collections.synchronizedSet(new HashSet<Closeable>());

		final class Mock implements Closeable {
			public Mock() {
				resources.add(this);
			}

			@Override
			public void close() throws IOException {
				resources.remove(this);
			}
		}

		ExecutorService threadPool = java.util.concurrent.Executors.newFixedThreadPool(10);
		try {
			for (int attempts = 0; attempts < 10000; attempts++) {

				try (SafeCloser closer = new SafeCloser()) {

					var finalResult = threadPool.submit(() -> {
						for (;;) {
							try {
								Mock mock = new Mock();
								Assert.assertSame(mock, closer.register(mock));
							} catch (ResourceClosedException e) {
								// Expected
								break;
							} catch (IOException e) {
								throw new AssertionError(e);
							}
						}
					});

					while (resources.isEmpty()) {
						Thread.yield();
					}

					closer.close();

					finalResult.get(3, TimeUnit.SECONDS); // should not throw

					Assert.assertTrue(resources.isEmpty());
				}
			}
		} finally {
			threadPool.shutdownNow();
		}
	}
}
