package org.vgcpge.eclipse.copilot.ui.internal;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

public class CopilotClient implements LanguageClient {
	private final LanguageClient delegate;
	private final CompletionStage<Void> initialized;

	public CopilotClient(LanguageClient delegate, CompletionStage<Void> initialized) {
		this.delegate = Objects.requireNonNull(delegate);
		this.initialized = initialized;
	}

	@Override
	public void telemetryEvent(Object object) {
		delegate.telemetryEvent(object);
	}

	@Override
	public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		delegate.publishDiagnostics(diagnostics);

	}

	@Override
	public void showMessage(MessageParams messageParams) {
		delegate.showMessage(messageParams);

	}

	@Override
	public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
		return delegate.showMessageRequest(requestParams);
	}

	@Override
	public void logMessage(MessageParams message) {
		delegate.logMessage(message);

	}

	@Override
	public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
		return delegate.applyEdit(params);
	}

	@Override
	public CompletableFuture<Void> registerCapability(RegistrationParams params) {
		return initialized.thenRun(() -> delegate.registerCapability(params)).toCompletableFuture();
	}

	@Override
	public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
		return delegate.unregisterCapability(params);
	}

	@Override
	public CompletableFuture<ShowDocumentResult> showDocument(ShowDocumentParams params) {
		return delegate.showDocument(params);
	}

	@Override
	public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
		return delegate.workspaceFolders();
	}

	@Override
	public CompletableFuture<List<Object>> configuration(ConfigurationParams configurationParams) {
		return delegate.configuration(configurationParams);
	}

	@Override
	public CompletableFuture<Void> createProgress(WorkDoneProgressCreateParams params) {
		return delegate.createProgress(params);
	}

	@Override
	public void notifyProgress(ProgressParams params) {
		delegate.notifyProgress(params);
	}

	@Override
	public void logTrace(LogTraceParams params) {
		delegate.logTrace(params);
	}

	@Override
	public CompletableFuture<Void> refreshSemanticTokens() {
		return delegate.refreshSemanticTokens();
	}

	@Override
	public CompletableFuture<Void> refreshCodeLenses() {
		return delegate.refreshCodeLenses();
	}

	@Override
	public CompletableFuture<Void> refreshInlayHints() {
		return delegate.refreshInlayHints();
	}

	@Override
	public CompletableFuture<Void> refreshInlineValues() {
		return delegate.refreshInlineValues();
	}

	@Override
	public CompletableFuture<Void> refreshDiagnostics() {
		return delegate.refreshDiagnostics();
	}

}
