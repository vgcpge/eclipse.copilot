package org.vgcpge.copilot.ls.rpc;

import java.util.List;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

public class Completions {
	@NonNull
	public List<CompletionItem> completions;
}
