package org.vgcpge.copilot.ls;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ShowDocumentParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.vgcpge.copilot.ls.rpc.CheckStatusOptions;
import org.vgcpge.copilot.ls.rpc.CopilotLanguageServer;
import org.vgcpge.copilot.ls.rpc.Status;

public class Authentication {
	private final LanguageClient client;
	private final CopilotLanguageServer languageServerDelegate;
	public Authentication(LanguageClient client, CopilotLanguageServer server) {
		super();
		this.client = client;
		this.languageServerDelegate = server;
		ensureAuthenticated();
	}
	
	private void ensureAuthenticated() {
		try {
			CheckStatusOptions options = new CheckStatusOptions();
			options.localChecksOnly = false;
			Status status = languageServerDelegate.checkStatus(options).get().status;
			switch (status) {
			case NotSignedIn: {
				authenticate();
				return;
			}
			case Normal:
			case InProgress:
			case OK:
				return;
			default:
				throw new IllegalStateException("Unknown status: " + status);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		} catch (Exception e) {
			var message = new StringWriter();
			try (PrintWriter printer = new PrintWriter(message)) {
				e.printStackTrace(printer);
			}
			client.logMessage(new MessageParams(MessageType.Error, message.toString()));
		}
	}

	private void authenticate() throws InterruptedException {
		try {
			var signInInitiateResult = languageServerDelegate.signInInitiate().get();
			long start = System.currentTimeMillis();
			
			ShowDocumentParams showDocumentParams = new ShowDocumentParams(signInInitiateResult.verificationUri);
			client.showDocument(showDocumentParams).get();
			
			
			String message = "Enter code " + signInInitiateResult.userCode + " on " + signInInitiateResult.verificationUri;
			client.showMessage(new MessageParams(MessageType.Warning, message));
			
			while (start + signInInitiateResult.expiresIn * 1000 < System.currentTimeMillis()) {
				Thread.sleep(signInInitiateResult.interval + 1000);
				Status status2 = languageServerDelegate.signInConfirm(signInInitiateResult.userCode).get().status;
				switch (status2) {
				case OK:
				case InProgress:
				case Normal:
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
}
