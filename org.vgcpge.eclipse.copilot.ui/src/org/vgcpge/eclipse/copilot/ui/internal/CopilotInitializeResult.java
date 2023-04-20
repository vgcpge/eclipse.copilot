package org.vgcpge.eclipse.copilot.ui.internal;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;

public class CopilotInitializeResult extends InitializeResult {
	@Override
	public ServerCapabilities getCapabilities() {
		var result = super.getCapabilities();
		if (result.getCompletionProvider() == null) {
			result.setCompletionProvider(new CompletionOptions());
		}
		return result;
	}
	
}
