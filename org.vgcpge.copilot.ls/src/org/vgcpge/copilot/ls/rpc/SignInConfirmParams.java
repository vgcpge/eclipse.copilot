package org.vgcpge.copilot.ls.rpc;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

public class SignInConfirmParams {
	@NonNull
	private String userCode;

	public SignInConfirmParams(String userCode) {
		this.userCode = Objects.requireNonNull(userCode);
	}
}
