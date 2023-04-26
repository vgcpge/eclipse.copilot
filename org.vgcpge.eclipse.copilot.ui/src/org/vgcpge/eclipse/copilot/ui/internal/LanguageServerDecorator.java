package org.vgcpge.eclipse.copilot.ui.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.FrameworkUtil;
import org.vgcpge.eclipse.copilot.ui.rpc.CheckStatusOptions;
import org.vgcpge.eclipse.copilot.ui.rpc.CheckStatusResult;
import org.vgcpge.eclipse.copilot.ui.rpc.CopilotLanguageServer;
import org.vgcpge.eclipse.copilot.ui.rpc.SignInConfrimResult;
import org.vgcpge.eclipse.copilot.ui.rpc.SignInInitiateResult;
import org.vgcpge.eclipse.copilot.ui.rpc.Status;

public class LanguageServerDecorator implements CopilotLanguageServer {
	private final CopilotLanguageServer languageServerDelegate;
	private static final String BUNDLE_ID = FrameworkUtil.getBundle(LanguageServerDecorator.class).getSymbolicName();

	public LanguageServerDecorator(CopilotLanguageServer languageServerDelegate) {
		super();
		this.languageServerDelegate = languageServerDelegate;
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		CompletableFuture<InitializeResult> result = languageServerDelegate.initialize(params);
		result.thenRun(() -> {
			ensureAuthenticated();
		});
		return result;
	}

	private void ensureAuthenticated() {
		try {
			CheckStatusOptions options = new CheckStatusOptions();
			options.localChecksOnly = false;
			Status status = checkStatus(options).get().status;
			switch (status) {
			case NotSignedIn: {
				authenticate();
				return;
			}
			case OK:
				return;
			default:
				throw new IllegalStateException("Unknown status: " + status);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new OperationCanceledException();
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	private void authenticate() throws InterruptedException {
		try {
			var signInInitiateResult = signInInitiate().get();
			long start = System.currentTimeMillis();
			PlatformUI.getWorkbench().getDisplay().syncExec( () -> {
				try {
					IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(0,
							BUNDLE_ID + ".authenticationRequest", "Copilot Authentication Request",
							"Enter the code from the modal dialog");
					browser.openURL(new URL(signInInitiateResult.verificationUri));
				} catch (MalformedURLException | PartInitException e) {
					throw new IllegalStateException(e);
				}
			});
			IStatus queryUser = org.eclipse.core.runtime.Status
					.warning("Copilot authentication request: " + signInInitiateResult.userCode);
			StatusManager.getManager().handle(queryUser, StatusManager.BLOCK | StatusManager.SHOW);
			while (start + signInInitiateResult.expiresIn * 1000 < System.currentTimeMillis()) {
				Thread.sleep(signInInitiateResult.interval + 1000);
				Status status2 = signInConfirm(signInInitiateResult.userCode).get().status;
				switch (status2) {
				case OK:
					return;
				case PromptUserDeviceFlow:
					continue;
				default:
					throw new IllegalStateException("Unknown status: " + status2);
				}
			}
			throw new IllegalStateException("Authenitcaton has timed out");
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		return languageServerDelegate.shutdown();
	}

	@Override
	public void exit() {
		languageServerDelegate.exit();
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return languageServerDelegate.getTextDocumentService();
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return languageServerDelegate.getWorkspaceService();
	}

	@Override
	public CompletableFuture<CheckStatusResult> checkStatus(CheckStatusOptions options) {
		return languageServerDelegate.checkStatus(options);
	}

	@Override
	public CompletableFuture<SignInInitiateResult> signInInitiate() {
		return languageServerDelegate.signInInitiate();
	}

	@Override
	public CompletableFuture<SignInConfrimResult> signInConfirm(String userCode) {
		return languageServerDelegate.signInConfirm(userCode);
	}

}
