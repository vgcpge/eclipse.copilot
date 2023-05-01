package org.vgcpge.copilot.ls;

import java.util.Objects;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/*
 * {
 * "position": { "line": 8, "character": 0 }, "insertSpaces": true, "tabSize":
 * 4, "uri": "file:///private/tmp/python_debugg/testing_python.py", "version": 0
 * } 
 */
@SuppressWarnings("unused") // fields are used by GSON
public class TextDocumentPositionParams extends org.eclipse.lsp4j.VersionedTextDocumentIdentifier {

	/**
	 * The position inside the text document.
	 */
	@NonNull
	private Position position;
	private boolean insertSpaces = false;

	private int tabSize = 4;

	public TextDocumentPositionParams(String uri, Position position, int version) {
		super(uri,  version);
		this.position = Objects.requireNonNull(position);
	}

}
