package org.vgcpge.copilot.ls;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
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
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.vgcpge.copilot.ls.rpc.CopilotLanguageServer;

import com.google.common.base.Throwables;

public class LanguageServerDecorator extends LanguageServerDecoratorBase {
	private final CompletableFuture<CopilotLanguageServer> languageServerDelegate;
	private final CompletableFuture<CopilotLanguageServer> initialized = new CompletableFuture<>();
	private Supplier<CopilotLanguageServer> startCopilot;

	public LanguageServerDecorator(Supplier<CopilotLanguageServer> startCopilot) {
		this(new CompletableFuture<>(), startCopilot);
	}

	private LanguageServerDecorator(CompletableFuture<CopilotLanguageServer> languageServerDelegate,
			Supplier<CopilotLanguageServer> startCopilot) {
		super(languageServerDelegate);
		this.languageServerDelegate = Objects.requireNonNull(languageServerDelegate);
		this.startCopilot = Objects.requireNonNull(startCopilot);
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		try {
			languageServerDelegate.complete(startCopilot.get());
		} catch (Exception e) {
			return createErrorResponse(ResponseErrorCode.ServerNotInitialized, e);
		}
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
				initialized.complete(languageServerDelegate.getNow(null));
			}
		});
		return result;
	}

	private static <T> CompletableFuture<T> createErrorResponse(ResponseErrorCode responseErrorCode, Exception e) {
		CompletableFuture<T> result = new CompletableFuture<T>();
		result.completeExceptionally(new ResponseErrorException(toResponseError(responseErrorCode, e)));
		return result;
	}

	private static ResponseError toResponseError(ResponseErrorCode responseErrorCode, Exception e) {
		return new ResponseError(responseErrorCode, e.getLocalizedMessage(), Throwables.getStackTraceAsString(e));
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
		return new TextDocumentServiceDecoratorBase(
				languageServerDelegate.thenApply(CopilotLanguageServer::getTextDocumentService)) {
			@Override
			public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
					CompletionParams position) {
				// getCompletionsCycling returns more completions than getCompletions()
				// See https://github.com/vgcpge/eclipse.copilot/issues/7
				return languageServerDelegate.thenCompose(s -> s.getCompletionsCycling(adaptCompletionParams(position)))
						.thenApply(c -> Either
								.forLeft(c.completions.stream().map(LanguageServerDecorator::adaptCompletoinItem)
										.distinct().collect(Collectors.toList())));
			}

			@Override
			public void didChange(DidChangeTextDocumentParams params) {
				params.getTextDocument().setVersion(1);
				super.didChange(params);
			}

		};
	}

	public CompletableFuture<CopilotLanguageServer> getInitialized() {
		return initialized;
	}

	private static org.vgcpge.copilot.ls.CompletionParams adaptCompletionParams(CompletionParams params) {
		TextDocumentIdentifier doc = params.getTextDocument();
		TextDocumentPositionParams docPos = new TextDocumentPositionParams(doc.getUri(), params.getPosition(), 1);
		return new org.vgcpge.copilot.ls.CompletionParams(docPos);
	}

	private static CompletionItem adaptCompletoinItem(org.vgcpge.copilot.ls.rpc.CompletionItem completion) {
		String text = completion.text;
		String displayText = completion.displayText;
		CompletionItem result = new CompletionItem(firstLine(displayText));
		// There is a bug in LSP4E that requires a HEAD element to be present. See #6.
		String html = "<head></head><body><pre>" + text.replace("\t", "  ") + "</pre></body>";
		result.setDetail(html);
		// Fixes #2. If filter is set, offset computation is based on substrings,
		// producing stable results.
		// See
		// org.eclipse.lsp4e.operations.completion.LSCompletionProposal.validate(IDocument,
		// int, DocumentEvent)
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
