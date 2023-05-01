package org.vgcpge.copilot.ls;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

public class TextDocumentServiceDecoratorBase implements TextDocumentService {

	private final CompletableFuture<TextDocumentService> delegate;

	public TextDocumentServiceDecoratorBase(CompletableFuture<TextDocumentService> delegate) {
		this.delegate = Objects.requireNonNull(delegate);
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		delegate.thenAccept(d -> d.didOpen(params));
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		delegate.thenAccept(d -> d.didChange(params));

	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		delegate.thenAccept(d -> d.didClose(params));

	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		delegate.thenAccept(d -> d.didSave(params));
	}
	
	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return delegate.thenCompose(d -> d.completion(position));
	}

}
