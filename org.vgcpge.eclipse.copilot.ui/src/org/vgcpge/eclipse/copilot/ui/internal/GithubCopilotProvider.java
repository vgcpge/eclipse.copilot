package org.vgcpge.eclipse.copilot.ui.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.vgcpge.copilot.ls.LanguageServer;
import org.vgcpge.copilot.ls.SafeCloser;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

@SuppressWarnings("resource")
public final class GithubCopilotProvider implements StreamConnectionProvider {
	private final SafeCloser closer = new SafeCloser();

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
				closer.register(new PipedOutputStream(input)), executorService));
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
		// LSP4E never poll the return value, so we can't return anything here -
		// unpolled error stream will lead to agent's hangup
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

	private static final Pattern AUTH_CODE_PATTERN = Pattern.compile("\\?userCode=([A-Z0-9\\-]+)");

	/**
	 * This is a job for LanguageServerClient, but LanguageServerClientImpl as it is
	 * not an API, so we can't configure it!
	 * https://github.com/eclipse/lsp4e/issues/626
	 */
	@Override
	public void handleMessage(Message message, org.eclipse.lsp4j.services.LanguageServer languageServer,
			java.net.URI rootURI) {
		if (message instanceof RequestMessage request) {
			if (request.getParams() instanceof ShowMessageRequestParams showMessage) {
				String text = showMessage.getMessage();
				Matcher matcher = AUTH_CODE_PATTERN.matcher(text);
				if (matcher.find()) {
					String code = matcher.group(1);
					putInClipboard(code);
					showMessage.setMessage(text + "\n" + "The code is also copied to clipboard.");
				}
			}
		}
	}

	private void putInClipboard(String text) {
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			Clipboard clipboard = new Clipboard(display);
			try {
				clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
			} finally {
				clipboard.dispose();
			}
		});
	};

	private void shutdownAndAwaitTermination() throws IOException {
		var pool = executorService;
//		// Disable new tasks from being submitted
		pool.shutdown();

		//		
//		try {
//			// Wait a while for existing tasks to terminate
//			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
//				pool.shutdownNow(); // Cancel currently executing tasks
//				// Wait a while for tasks to respond to being cancelled
//				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
//					throw new IOException("Pool did not terminate");
//			}
//		} catch (InterruptedException ex) {
//			// (Re-)Cancel if current thread also interrupted
//			pool.shutdownNow();
//			// Preserve interrupt status
//			Thread.currentThread().interrupt();
//		}
	}
}
