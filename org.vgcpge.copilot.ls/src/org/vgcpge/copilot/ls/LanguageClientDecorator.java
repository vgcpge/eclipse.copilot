package org.vgcpge.copilot.ls;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageClient;
import org.vgcpge.copilot.ls.rpc.CopilotMessageParams;

public class LanguageClientDecorator extends DelegatingLanguageClient {

	private CompletableFuture<Void> initialized;

	public LanguageClientDecorator(CompletableFuture<LanguageClient> delegate, CompletableFuture<Void> initialized) {
		super(delegate);
		this.initialized = initialized;
	}

	@Override
	public CompletableFuture<Void> registerCapability(RegistrationParams params) {
		return initialized.thenRun(() -> super.registerCapability(params));
	}
	
	@JsonNotification
	public void LogMessage(CopilotMessageParams params) {
		MessageParams messageParams = new MessageParams(MessageType.forValue(4 - params.level), params.message);
		super.logMessage(messageParams);
	}
	
	@SuppressWarnings("unused")
	@JsonNotification
	public void statusNotification(Object status, Object message) {
		
		return; // suppress as LSP4e does not support progress notifications without job identifiers
	}
	
}