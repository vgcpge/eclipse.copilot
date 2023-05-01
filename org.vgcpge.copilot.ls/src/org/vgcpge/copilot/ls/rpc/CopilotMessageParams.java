package org.vgcpge.copilot.ls.rpc;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

public class CopilotMessageParams {
	@NonNull
	public int level;
	public String message;
}
