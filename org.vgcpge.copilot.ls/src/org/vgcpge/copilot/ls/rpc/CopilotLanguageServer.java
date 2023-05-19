package org.vgcpge.copilot.ls.rpc;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;
import org.vgcpge.copilot.ls.CompletionParams;

public interface CopilotLanguageServer extends LanguageServer {
	
	/** {"method":"checkStatus","id":3,"jsonrpc":"2.0","params":{"options":{"localChecksOnly":true}}} **/
	@JsonRequest
	CompletableFuture<CheckStatusResult> checkStatus(CheckStatusOptions options);
	
	/**{"method":"signInInitiate","id":5,"jsonrpc":"2.0","params":{}}**/
	@JsonRequest
	CompletableFuture<SignInInitiateResult> signInInitiate();
	
	/** {"method":"signInConfirm","id":6,"jsonrpc":"2.0","params":{"userCode":"3E52-C586"}} **/
	@JsonRequest
	CompletableFuture<CheckStatusResult> signInConfirm(SignInConfirmParams param);
	
	@JsonRequest
	CompletableFuture<Completions> getCompletions(CompletionParams param);

	@JsonRequest
	CompletableFuture<Completions> getCompletionsCycling(CompletionParams param);
}
