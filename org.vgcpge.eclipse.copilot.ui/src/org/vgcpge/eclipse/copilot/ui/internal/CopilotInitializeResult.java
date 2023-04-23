package org.vgcpge.eclipse.copilot.ui.internal;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;

public class CopilotInitializeResult extends InitializeResult {
	@Override
	public ServerCapabilities getCapabilities() {
		var result = super.getCapabilities();
		CompletionOptions completionProvider = result.getCompletionProvider();
		if (completionProvider == null) {
			result.setCompletionProvider(completionProvider = new CompletionOptions());
		}
		if (completionProvider.getResolveProvider() == null) {
			completionProvider.setResolveProvider(Boolean.FALSE);
		}
		return result;
	}
	
}
