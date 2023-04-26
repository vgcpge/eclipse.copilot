package org.vgcpge.eclipse.copilot.ui.rpc;

/** {"jsonrpc":"2.0","id":5,"result":{"status":"PromptUserDeviceFlow","userCode":"3E52-C586","verificationUri":"https://github.com/login/device","expiresIn":899,"interval":5}} **/
public class SignInInitiateResult extends CheckStatusResult {
	public String userCode;
	public String verificationUri;
	public int interval = 10;
	public int expiresIn = 500;
}
