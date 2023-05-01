package org.vgcpge.copilot.ls;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.vgcpge.copilot.ls.rpc.CopilotLanguageServer;

public class LanguageServerDecorator extends DelegatingLanguageServer {
	private final CompletableFuture<CopilotLanguageServer> languageServerDelegate;
	private final CompletableFuture<Void> initialized = new CompletableFuture<>();
	
	public LanguageServerDecorator(CompletableFuture<CopilotLanguageServer> downstreamServer) {
		super(downstreamServer);
		this.languageServerDelegate = Objects.requireNonNull(downstreamServer);
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		CompletableFuture<InitializeResult> result = super.initialize(params).thenApply(result2 -> {
			CompletionOptions completionProvider = result2.getCapabilities().getCompletionProvider();
			if (completionProvider == null) {
				result2.getCapabilities().setCompletionProvider(completionProvider = new CompletionOptions());
			}
			if (completionProvider.getResolveProvider() == null) {
				completionProvider.setResolveProvider(Boolean.FALSE);
			}
			return result2;
		});
		result.whenCompleteAsync((ignore, error) -> {
			if (error != null) {
				initialized.completeExceptionally(error);
			} else {
				initialized.complete(null);
			}
		});
		return result;
	}



	@Override
	public CompletableFuture<Object> shutdown() {
		return super.shutdown();
	}

	@Override
	public void exit() {
		super.exit();
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return new TextDocumentServiceDecorator(super.getTextDocumentService()) {
			@Override
			public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
					CompletionParams position) {
				return languageServerDelegate.thenCompose(s -> s.getCompletions(adaptCompletionParams(position)))
						.thenApply(c -> Either.forLeft(c.completions.stream().map(LanguageServerDecorator::adaptCompletoinItem)
								.collect(Collectors.toList())));
			}
			
			@Override
			public void didChange(DidChangeTextDocumentParams params) {
				params.getTextDocument().setVersion(1);
				super.didChange(params);
			}

		};
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return super.getWorkspaceService();
	}

	public CompletableFuture<Void> getInitialized() {
		return initialized;
	}

	private static org.vgcpge.copilot.ls.CompletionParams adaptCompletionParams(CompletionParams params) {
		TextDocumentIdentifier doc = params.getTextDocument();
		TextDocumentPositionParams docPos = new TextDocumentPositionParams(doc.getUri(), params.getPosition(), 1, false);
		return new org.vgcpge.copilot.ls.CompletionParams(docPos);
	}

	private static CompletionItem adaptCompletoinItem(org.vgcpge.copilot.ls.rpc.CompletionItem completion) {
		String text = completion.text;
		String displayText = completion.displayText;
		CompletionItem result = new CompletionItem(firstLine(displayText));
		String html = "<pre>"+text.replace("\t", "  ")+"</pre>";
		result.setDetail(html);
		// Fixes #2. If filter is set, offset computation is based on substrings, producing stable results.
		// See org.eclipse.lsp4e.operations.completion.LSCompletionProposal.validate(IDocument, int, DocumentEvent)
		result.setFilterText(text); 
		TextEdit textEdit = new TextEdit(completion.range, text);
		result.setTextEdit(Either.forLeft(textEdit));
		return result;
	}
	
	private static String firstLine(String message) {
		message = message.strip();
		int position = message.indexOf('\n');
		if (position <= 0) {
			return message;
		}
		return message.substring(0, position);
	}

}
