package org.vgcpge.eclipse.copilot.ui.internal;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.services.LanguageServer;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.services.LanguageServer;

public class GithubCopilotProvider extends ProcessStreamConnectionProvider {
	public GithubCopilotProvider() {
		super(Arrays.asList("node",
				System.getProperty("user.home") + "/.config/nvim/pack/github/start/copilot.vim/copilot/dist/agent.js"),
				Paths.get(URI.create(System.getProperty("osgi.instance.area"))).toString());
	}
	
	@Override
	public void handleMessage(Message message, LanguageServer languageServer, @Nullable URI rootURI) {
		if (message instanceof ResponseMessage responseMessage) {
			if (responseMessage.getResult() instanceof InitializeResult) {
			}
		}
	}
	
}
