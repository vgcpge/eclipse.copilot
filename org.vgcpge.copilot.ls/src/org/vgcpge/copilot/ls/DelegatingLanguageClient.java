package org.vgcpge.copilot.ls;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.LogTraceParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ShowDocumentParams;
import org.eclipse.lsp4j.ShowDocumentResult;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;

public class DelegatingLanguageClient implements LanguageClient {
	private final CompletableFuture<LanguageClient> delegate;

	public DelegatingLanguageClient(CompletableFuture<LanguageClient> delegate) {
		this.delegate = Objects.requireNonNull(delegate);
	}

	@Override
	public void telemetryEvent(Object object) {
		delegate.thenAccept(d -> d.telemetryEvent(object));
	}

	@Override
	public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		delegate.thenAccept(d -> d.publishDiagnostics(diagnostics));

	}

	@Override
	public void showMessage(MessageParams messageParams) {
		delegate.thenAccept(d -> d.showMessage(messageParams));

	}

	@Override
	public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
		return delegate.thenCompose(d -> d.showMessageRequest(requestParams));
	}

	@Override
	public void logMessage(MessageParams message) {
		delegate.thenAccept(d -> d.logMessage(message));

	}

	@Override
	public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
		return delegate.thenCompose(d -> d.applyEdit(params));
	}

	@Override
	public CompletableFuture<Void> registerCapability(RegistrationParams params) {
		return delegate.thenCompose(d -> d.registerCapability(params));
	}

	@Override
	public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
		return delegate.thenAccept(d -> d.unregisterCapability(params));
	}

	@Override
	public CompletableFuture<ShowDocumentResult> showDocument(ShowDocumentParams params) {
		return delegate.thenCompose(d -> d.showDocument(params));
	}

	@Override
	public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
		return delegate.thenCompose(d -> d.workspaceFolders());
	}

	@Override
	public CompletableFuture<List<Object>> configuration(ConfigurationParams configurationParams) {
		return delegate.thenCompose(d -> d.configuration(configurationParams));
	}

	@Override
	public CompletableFuture<Void> createProgress(WorkDoneProgressCreateParams params) {
		return delegate.thenCompose(d -> d.createProgress(params));
	}

	@Override
	public void notifyProgress(ProgressParams params) {
		delegate.thenAccept(d -> d.notifyProgress(params));
	}

	@Override
	public void logTrace(LogTraceParams params) {
		delegate.thenAccept(d -> d.logTrace(params));
	}

	@Override
	public CompletableFuture<Void> refreshSemanticTokens() {
		return delegate.thenCompose(d -> d.refreshSemanticTokens());
	}

	@Override
	public CompletableFuture<Void> refreshCodeLenses() {
		return delegate.thenCompose(d -> d.refreshCodeLenses());
	}

	@Override
	public CompletableFuture<Void> refreshInlayHints() {
		return delegate.thenCompose(d -> d.refreshInlayHints());
	}

	@Override
	public CompletableFuture<Void> refreshInlineValues() {
		return delegate.thenCompose(d -> d.refreshInlineValues());
	}

	@Override
	public CompletableFuture<Void> refreshDiagnostics() {
		return delegate.thenCompose(d -> d.refreshDiagnostics());
	}

}
