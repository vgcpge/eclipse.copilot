package org.vgcpge.copilot.ls;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.SetTraceParams;
import org.eclipse.lsp4j.WorkDoneProgressCancelParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.vgcpge.copilot.ls.rpc.CopilotLanguageServer;

/** Delegate all invocations to the wrapped object without any changes. **/
public class LanguageServerDecoratorBase implements LanguageServer {
	private final CompletableFuture<CopilotLanguageServer> delegate;
	private final DelegatingWorkspaceService workspaceService;

	public LanguageServerDecoratorBase(CompletableFuture<CopilotLanguageServer> downstreamServer) {
		this.delegate = Objects.requireNonNull(downstreamServer);
		this.workspaceService = new DelegatingWorkspaceService(downstreamServer.thenApply(LanguageServer::getWorkspaceService));
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		return delegate.thenCompose(d -> d.initialize(params));
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		return delegate.thenCompose(d -> d.shutdown());
	}

	@Override
	public void exit() {
		delegate.thenAccept(d -> d.exit());
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return new TextDocumentServiceDecoratorBase(delegate.thenApply(LanguageServer::getTextDocumentService));
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return workspaceService;
	}

	@Override
	public void initialized(InitializedParams params) {
		delegate.thenAccept(d -> d.initialized(params));
	}

	@Override
	public void cancelProgress(WorkDoneProgressCancelParams params) {
		delegate.thenAccept(d -> d.cancelProgress(params));
	}

	@Override
	public void setTrace(SetTraceParams params) {
		delegate.thenAccept(d -> d.setTrace(params));
	}

}