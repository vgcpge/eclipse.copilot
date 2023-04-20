package org.vgcpge.eclipse.copilot.ui.internal;

import java.util.Map;
import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.Launcher.Builder;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.services.LanguageServer;

public class LauncherBuilder extends Launcher.Builder<LanguageServer> {
	{
		wrapMessages(this::hookCompletion2);
	}
	
	@Override
	protected Map<String, JsonRpcMethod> getSupportedMethods() {
		var result = super.getSupportedMethods();
		result.compute("initialize", this::hookInitialize);
		return result;
	}
	
	public Builder<LanguageServer> wrapMessages(Function<MessageConsumer, MessageConsumer> wrapper) {
		var original = this.messageWrapper;
		if (original != null) {
			this.messageWrapper = original.andThen(wrapper);
		} else {
			this.messageWrapper = wrapper;
		}
		return this;
	}	
	

	private JsonRpcMethod hookInitialize(String methodName, JsonRpcMethod original) {
		assert "initialize".equals(methodName);
		assert methodName.equals(original.getMethodName());
		// Override default values for return type, to compensate for Copilot language server non-compilance
		return JsonRpcMethod.request(original.getMethodName(), CopilotInitializeResult.class, original.getParameterTypes());
		
	}

	private JsonRpcMethod hookCompletion(String methodName, JsonRpcMethod original) {
		assert "textDocument/completion".equals(methodName);
		assert methodName.equals(original.getMethodName());
		// Copilot says Method not found: textDocument/completion
		// Nvim uses getCompletions for the same function
		return JsonRpcMethod.request("getCompletions", original.getReturnType(), original.getParameterTypes());
	}

	private MessageConsumer hookCompletion2(MessageConsumer consumer) {
		return message -> {
			if (message instanceof RequestMessage) {
				var request = (RequestMessage) message;
				if ("textDocument/completion".equals(request.getMethod())) {
					// This does not work: Invalid params:  must have required property 'doc'
					request.setMethod("getCompletions");
				}
			}
			consumer.consume(message);
		};
	}
	
	

}
