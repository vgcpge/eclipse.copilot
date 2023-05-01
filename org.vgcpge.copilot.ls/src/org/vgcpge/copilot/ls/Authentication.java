package org.vgcpge.copilot.ls;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ShowDocumentParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.vgcpge.copilot.ls.rpc.CheckStatusOptions;
import org.vgcpge.copilot.ls.rpc.CopilotLanguageServer;
import org.vgcpge.copilot.ls.rpc.SignInConfirmParams;
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

			signInInitiateResult.verificationUri += "?userCode=" + signInInitiateResult.userCode;
			ShowDocumentParams showDocumentParams = new ShowDocumentParams(signInInitiateResult.verificationUri);
			showDocumentParams.setSelection(new Range(new Position(), new Position()));
			client.showDocument(showDocumentParams).get();

			String message = "To sign in Github Copilot, enter code " + signInInitiateResult.userCode + " on "
					+ signInInitiateResult.verificationUri
					+ ". For your convenience code has been appended to the URL.";

			ShowMessageRequestParams requestParams = new ShowMessageRequestParams();
			MessageActionItem okAction = new MessageActionItem("OK");
			MessageActionItem cancelAction = new MessageActionItem("Cancel");
			requestParams.setActions(List.of(okAction, cancelAction));
			requestParams.setMessage(message);
			requestParams.setType(MessageType.Warning);

			if (!okAction.equals(client.showMessageRequest(requestParams).get())) {
				return;
			}

			while (start + signInInitiateResult.expiresIn * 1000 > System.currentTimeMillis()) {
				Thread.sleep(signInInitiateResult.interval + 1000);
				Status status2 = languageServerDelegate
						.signInConfirm(new SignInConfirmParams(signInInitiateResult.userCode)).get().status;
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
