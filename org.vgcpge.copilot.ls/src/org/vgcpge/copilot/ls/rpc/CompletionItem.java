package org.vgcpge.copilot.ls.rpc;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

public class CompletionItem {
	@NonNull
	public String text;
	@NonNull
	public String displayText;
	@NonNull
	public Range range;
	
}
