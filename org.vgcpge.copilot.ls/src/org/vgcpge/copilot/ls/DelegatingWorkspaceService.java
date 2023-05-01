package org.vgcpge.copilot.ls;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CreateFilesParams;
import org.eclipse.lsp4j.DeleteFilesParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.WorkspaceDiagnosticParams;
import org.eclipse.lsp4j.WorkspaceDiagnosticReport;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.services.WorkspaceService;

public class DelegatingWorkspaceService implements WorkspaceService {

	private final CompletableFuture<WorkspaceService> delegate;

	public DelegatingWorkspaceService(CompletableFuture<WorkspaceService> delegate) {
		this.delegate = Objects.requireNonNull(delegate);
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		delegate.thenAccept(d -> d.didChangeConfiguration(params));
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		delegate.thenAccept(d -> d.didChangeWatchedFiles(params));
	}

	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		return delegate.thenCompose(d -> d.executeCommand(params));
	}

	@Override
	public CompletableFuture<WorkspaceSymbol> resolveWorkspaceSymbol(WorkspaceSymbol workspaceSymbol) {
		return delegate.thenCompose(d -> d.resolveWorkspaceSymbol(workspaceSymbol));
	}

	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		delegate.thenAccept(d -> d.didChangeWorkspaceFolders(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> willCreateFiles(CreateFilesParams params) {
		return delegate.thenCompose(d -> d.willCreateFiles(params));
	}

	@Override
	public void didCreateFiles(CreateFilesParams params) {
		delegate.thenAccept(d -> d.didCreateFiles(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> willRenameFiles(RenameFilesParams params) {
		return delegate.thenCompose(d -> d.willRenameFiles(params));
	}

	@Override
	public void didRenameFiles(RenameFilesParams params) {
		delegate.thenAccept(d -> d.didRenameFiles(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> willDeleteFiles(DeleteFilesParams params) {
		return delegate.thenCompose(d -> d.willDeleteFiles(params));
	}

	@Override
	public void didDeleteFiles(DeleteFilesParams params) {
		delegate.thenAccept(d -> d.didDeleteFiles(params));
	}

	@Override
	public CompletableFuture<WorkspaceDiagnosticReport> diagnostic(WorkspaceDiagnosticParams params) {
		return delegate.thenCompose(d -> d.diagnostic(params));
	}

}
