package org.vgcpge.eclipse.copilot.ui.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.Launcher.Builder;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.services.LanguageClient;
import org.vgcpge.eclipse.copilot.ui.rpc.CopilotLanguageServer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LauncherBuilder extends Launcher.Builder<CopilotLanguageServer> {
	private final Map<String, Function<Object, Object>> responseHooks = Collections.synchronizedMap(new HashMap<>());
	private MessageJsonHandler jsonHandler;

	{
		wrapMessages(this::hookMessages);
	}

	@Override
	protected MessageJsonHandler createJsonHandler() {
		return this.jsonHandler = super.createJsonHandler();
	}

	@Override
	protected Map<String, JsonRpcMethod> getSupportedMethods() {
		var result = super.getSupportedMethods();
		result.compute("initialize", this::hookInitialize);
		return result;
	}

	public Builder<CopilotLanguageServer> wrapMessages(Function<MessageConsumer, MessageConsumer> wrapper) {
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
		// Copilot does not use the LSP completion protocol, so it does not declare to be compatible with it
		// LSP4E completions would not work with non-compatible servers, so we imitate completion capability by changing defaults for server response
		return JsonRpcMethod.request(original.getMethodName(), CopilotInitializeResult.class,
				original.getParameterTypes());

	}

	private MessageConsumer hookMessages(MessageConsumer consumer) {
		return message -> {
			if (message instanceof RequestMessage) {
				message = adaptRequest((RequestMessage) message);
			} else if (message instanceof NotificationMessage) {
				NotificationMessage notification = (NotificationMessage) message;
				if ("statusNotification".equals(notification.getMethod())) {
					// LSP4E does not seem to support progress without jobs
					// It does not make sense to log this, so lets suppress
					// Convert to window/logMessage or similar if needed later.
					return;
				}
				message = adaptNotification(notification);
			} else if (message instanceof ResponseMessage) {
				message = adaptResponse((ResponseMessage) message);
			}
			consumer.consume(message);
		};
	}

	private ResponseMessage adaptResponse(ResponseMessage response) {
		Function<Object, Object> hook = responseHooks.remove(response.getId());
		if (hook != null) {
			response.setResult(hook.apply(response.getResult()));
		}
		return response;
	}

	private NotificationMessage adaptNotification(NotificationMessage notification) {
		String method = notification.getMethod();
		if ("LogMessage".equals(method)) {
			notification.setMethod("window/logMessage");
			notification.setParams(adaptMessageParams(notification.getParams()));
		} else if ("textDocument/didChange".equals(method)) {
			DidChangeTextDocumentParams params = (DidChangeTextDocumentParams)notification.getParams();
			// CompletionParams produced by Eclipse do not include document version, so version mismatch happens, is different versions are sent here
			params.getTextDocument().setVersion(1);
		}
		return notification;
	}

	private MessageParams adaptMessageParams(Object input) {
		JsonObject json = (JsonObject) input;
		return new MessageParams(MessageType.forValue(4 - json.getAsJsonPrimitive("level").getAsInt()),
				json.getAsJsonPrimitive("message").getAsString());
	}

	private RequestMessage adaptRequest(RequestMessage request) {
		// Copilot does not accept standard method for completions
		if ("textDocument/completion".equals(request.getMethod())) {
			// This does not work: Invalid params: must have required property 'doc'
			request.setMethod("getCompletions");
			request.setParams(adaptCompletionParams((CompletionParams) request.getParams()));
			responseHooks.put(request.getId(), this::adaptCompletionResponse);
		}
		return request;
	}

	/**
	 * 
	 * { "jsonrpc": "2.0", "id": 3, "method": "getCompletions", "params": { "doc": {
	 * "position": { "line": 8, "character": 0 }, "insertSpaces": true, "tabSize":
	 * 4, "uri": "file:///private/tmp/python_debugg/testing_python.py", "version": 0
	 * } } }
	 * 
	 */
	private Object adaptCompletionParams(CompletionParams params) {
		TextDocumentIdentifier textDocument = params.getTextDocument();
		TextDocumentPositionParams doc = new TextDocumentPositionParams(textDocument.getUri(), params.getPosition(), 1, false);
		return new org.vgcpge.eclipse.copilot.ui.internal.CompletionParams(doc);
	}

	private Either<List<CompletionItem>, CompletionList> adaptCompletionResponse(Object input) {
		if (input instanceof JsonObject) {
			JsonObject json = (JsonObject) input;
			List<CompletionItem> result = new ArrayList<>();
			json.getAsJsonArray("completions").forEach(i -> result.add(adaptCompletionItem(i)));
			return Either.forLeft(result);
		}
		return (Either<List<CompletionItem>, CompletionList>) input;
	}

	private CompletionItem adaptCompletionItem(JsonElement i) {
		// {"uuid":"1bc04aaa-70bd-44cb-9f09-0059cb322a6c","text":"\t\tSystem.out.println(\"Hello
		// World!\");","range":{"start":{"line":5,"character":0},"end":{"line":5,"character":2}},"displayText":"System.out.println(\"Hello
		// World!\");","position":{"line":5,"character":2},"docVersion":1}]}
		Gson gson = jsonHandler.getGson();
		var result = gson.fromJson(i, CompletionItem.class);
		JsonObject object = i.getAsJsonObject();
		String text = object.get("text").getAsString();
		String displayText = object.get("displayText").getAsString();
		result.setLabel(firstLine(displayText));
		String html = "<pre>"+text.replace("\t", "  ")+"</pre>";
		result.setDetail(html);
		// Fixes #2. If filter is set, offset computation is based on substrings, producing stable results.
		// See org.eclipse.lsp4e.operations.completion.LSCompletionProposal.validate(IDocument, int, DocumentEvent)
		result.setFilterText(text); 
		TextEdit textEdit = new TextEdit(gson.fromJson(object.get("range"), Range.class), text);
		result.setTextEdit(Either.forLeft(textEdit));
		return result;
	}

	private static String firstLine(String message) {
		message = message.strip();
		int position = message.indexOf('\n');
		if (position <= 0) {
			return message;
		}
		return message.substring(0, position);
	}
	
	@Override
	protected RemoteEndpoint createRemoteEndpoint(MessageJsonHandler jsonHandler) {
		setLocalServices(localServices.stream().map(this::wrapLocalService).collect(Collectors.toUnmodifiableList()));
		return super.createRemoteEndpoint(jsonHandler);
	}
	private CompletableFuture<Void> initialized;
	
	@Override
	protected CopilotLanguageServer createProxy(RemoteEndpoint remoteEndpoint) {
		LanguageServerDecorator decorator = new LanguageServerDecorator(super.createProxy(remoteEndpoint));
		decorator.getInitialized().whenComplete((ignored, error) -> {
			if (error != null) {
				initialized.completeExceptionally(error);
			} else {
				initialized.complete(null);
			}
		});
		return decorator;
	}

	private Object wrapLocalService(Object object1) {
		if (object1 instanceof LanguageClient) {
			return new CopilotClient((LanguageClient)object1, initialized = new CompletableFuture<>());
		}
		return object1;
	}

}
