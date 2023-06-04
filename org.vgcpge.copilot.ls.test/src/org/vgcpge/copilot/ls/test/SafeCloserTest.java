package org.vgcpge.copilot.ls.test;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
	@Test
	public void concurrentClosing() throws IOException, InterruptedException, ExecutionException {
		List<Closeable> resources = Collections.synchronizedList(new ArrayList<Closeable>());

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

		for (int attempts = 0; attempts < 100; attempts++) {

			try (SafeCloser closer = new SafeCloser()) {

				var finalResult = threadPool.submit(() -> {
					List<Future<?>> results = new ArrayList<>();
					for (int i = 0; i < 100; i++) {
						results.add(threadPool.submit(() -> {
							try {
								Mock mock = new Mock();
								Assert.assertSame(mock, closer.register(mock));
							} catch (ResourceClosedException e) {
								// Expected
							} catch (IOException e) {
								throw new AssertionError(e);
							}
						}));
					}
					for (Future<?> result : results) {
						try {
							result.get(); // should not throw
						} catch (InterruptedException | ExecutionException e) {
							throw new AssertionError(e);
						}
					}
				});

				while (resources.isEmpty()) {
					Thread.yield();
				}

				closer.close();

				finalResult.get(); // should not throw

				Assert.assertTrue(resources.isEmpty());
			}
		}
	}
}
