package org.vgcpge.eclipse.copilot.ui.internal;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.SetTraceParams;
import org.eclipse.lsp4j.WorkDoneProgressCancelParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.NotebookDocumentService;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.vgcpge.eclipse.copilot.ui.rpc.CopilotLanguageServer;

public class DelegatingLanguageServer implements LanguageServer {
	private final CopilotLanguageServer delegate;

	public DelegatingLanguageServer(CopilotLanguageServer languageServerDelegate) {
		this.delegate = Objects.requireNonNull(languageServerDelegate);
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		return delegate.initialize(params);
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		return delegate.shutdown();
	}

	@Override
	public void exit() {
		delegate.exit();
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return delegate.getTextDocumentService();
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return delegate.getWorkspaceService();
	}

	@Override
	public void initialized(InitializedParams params) {
		delegate.initialized(params);
	}

	@Override
	public NotebookDocumentService getNotebookDocumentService() {
		return delegate.getNotebookDocumentService();
	}

	@Override
	public void cancelProgress(WorkDoneProgressCancelParams params) {
		delegate.cancelProgress(params);
	}

	@Override
	public void setTrace(SetTraceParams params) {
		delegate.setTrace(params);
	}

}