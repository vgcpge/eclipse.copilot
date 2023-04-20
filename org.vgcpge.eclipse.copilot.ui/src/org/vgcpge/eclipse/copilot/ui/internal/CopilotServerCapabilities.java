package org.vgcpge.eclipse.copilot.ui.internal;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.ServerCapabilities;

public class CopilotServerCapabilities extends ServerCapabilities {
	{
		setCompletionProvider(new CompletionOptions());
	}

}
