package org.vgcpge.copilot.ls;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyIncomingCallsParams;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.CallHierarchyOutgoingCallsParams;
import org.eclipse.lsp4j.CallHierarchyPrepareParams;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.ColorPresentation;
import org.eclipse.lsp4j.ColorPresentationParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentColorParams;
import org.eclipse.lsp4j.DocumentDiagnosticParams;
import org.eclipse.lsp4j.DocumentDiagnosticReport;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.ImplementationParams;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.InlineValue;
import org.eclipse.lsp4j.InlineValueParams;
import org.eclipse.lsp4j.LinkedEditingRangeParams;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Moniker;
import org.eclipse.lsp4j.MonikerParams;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SelectionRangeParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensDelta;
import org.eclipse.lsp4j.SemanticTokensDeltaParams;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensRangeParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
import org.eclipse.lsp4j.WillSaveTextDocumentParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.TextDocumentService;

public class TextDocumentServiceDecorator implements TextDocumentService {

	private final TextDocumentService delegate;

	public TextDocumentServiceDecorator(TextDocumentService textDocumentService) {
		this.delegate = Objects.requireNonNull(textDocumentService);
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		delegate.didOpen(params);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		delegate.didChange(params);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		delegate.didClose(params);
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		delegate.didSave(params);
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return delegate.completion(position);
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return delegate.resolveCompletionItem(unresolved);
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return delegate.hover(params);
	}

	@Override
	public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
		return delegate.signatureHelp(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> declaration(
			DeclarationParams params) {
		return delegate.declaration(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		return delegate.definition(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(
			TypeDefinitionParams params) {
		return delegate.typeDefinition(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> implementation(
			ImplementationParams params) {
		return delegate.implementation(params);
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return delegate.references(params);
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		return delegate.documentHighlight(params);
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		return delegate.documentSymbol(params);
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		return delegate.codeAction(params);
	}

	@Override
	public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
		return delegate.resolveCodeAction(unresolved);
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		return delegate.codeLens(params);
	}

	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		return delegate.resolveCodeLens(unresolved);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return delegate.formatting(params);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return delegate.rangeFormatting(params);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
		return delegate.onTypeFormatting(params);
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return delegate.rename(params);
	}

	@Override
	public CompletableFuture<LinkedEditingRanges> linkedEditingRange(LinkedEditingRangeParams params) {
		return delegate.linkedEditingRange(params);
	}

	@Override
	public void willSave(WillSaveTextDocumentParams params) {
		delegate.willSave(params);
	}

	@Override
	public CompletableFuture<List<TextEdit>> willSaveWaitUntil(WillSaveTextDocumentParams params) {
		return delegate.willSaveWaitUntil(params);
	}

	@Override
	public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
		return delegate.documentLink(params);
	}

	@Override
	public CompletableFuture<DocumentLink> documentLinkResolve(DocumentLink params) {
		return delegate.documentLinkResolve(params);
	}

	@Override
	public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
		return delegate.documentColor(params);
	}

	@Override
	public CompletableFuture<List<ColorPresentation>> colorPresentation(ColorPresentationParams params) {
		return delegate.colorPresentation(params);
	}

	@Override
	public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
		return delegate.foldingRange(params);
	}

	@Override
	public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
			PrepareRenameParams params) {
		return delegate.prepareRename(params);
	}

	@Override
	public CompletableFuture<List<TypeHierarchyItem>> prepareTypeHierarchy(TypeHierarchyPrepareParams params) {
		return delegate.prepareTypeHierarchy(params);
	}

	@Override
	public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySupertypes(TypeHierarchySupertypesParams params) {
		return delegate.typeHierarchySupertypes(params);
	}

	@Override
	public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySubtypes(TypeHierarchySubtypesParams params) {
		return delegate.typeHierarchySubtypes(params);
	}

	@Override
	public CompletableFuture<List<CallHierarchyItem>> prepareCallHierarchy(CallHierarchyPrepareParams params) {
		return delegate.prepareCallHierarchy(params);
	}

	@Override
	public CompletableFuture<List<CallHierarchyIncomingCall>> callHierarchyIncomingCalls(
			CallHierarchyIncomingCallsParams params) {
		return delegate.callHierarchyIncomingCalls(params);
	}

	@Override
	public CompletableFuture<List<CallHierarchyOutgoingCall>> callHierarchyOutgoingCalls(
			CallHierarchyOutgoingCallsParams params) {
		return delegate.callHierarchyOutgoingCalls(params);
	}

	@Override
	public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
		return delegate.selectionRange(params);
	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
		return delegate.semanticTokensFull(params);
	}

	@Override
	public CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> semanticTokensFullDelta(
			SemanticTokensDeltaParams params) {
		return delegate.semanticTokensFullDelta(params);
	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensRange(SemanticTokensRangeParams params) {
		return delegate.semanticTokensRange(params);
	}

	@Override
	public CompletableFuture<List<Moniker>> moniker(MonikerParams params) {
		return delegate.moniker(params);
	}

	@Override
	public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
		return delegate.inlayHint(params);
	}

	@Override
	public CompletableFuture<InlayHint> resolveInlayHint(InlayHint unresolved) {
		return delegate.resolveInlayHint(unresolved);
	}

	@Override
	public CompletableFuture<List<InlineValue>> inlineValue(InlineValueParams params) {
		return delegate.inlineValue(params);
	}

	@Override
	public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
		return delegate.diagnostic(params);
	}

}
