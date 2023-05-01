package org.vgcpge.copilot.ls.rpc;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**  {"jsonrpc":"2.0","id":3,"result":{"status":"NotSignedIn"}}
 *   {"jsonrpc":"2.0","id":"2","result":{"status":"OK","user":"vgcpge"}} 
 *  **/
public class CheckStatusResult {
	@NonNull
	public Status status;
	public String user;
}
