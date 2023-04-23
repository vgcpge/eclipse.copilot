package org.vgcpge.eclipse.copilot.ui.internal;

import java.util.Objects;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/*
 * {
 * "position": { "line": 8, "character": 0 }, "insertSpaces": true, "tabSize":
 * 4, "uri": "file:///private/tmp/python_debugg/testing_python.py", "version": 0
 * } 
 */
public class TextDocumentPositionParams extends org.eclipse.lsp4j.VersionedTextDocumentIdentifier {

	/**
	 * The position inside the text document.
	 */
	@NonNull
	private Position position;
	private boolean insertSpaces = true;

	private int tabSize = 4;

	public TextDocumentPositionParams(String uri, Position position, int version, boolean insertSpaces) {
		super(uri,  version);
		this.position = Objects.requireNonNull(position);
		this.insertSpaces = insertSpaces;
		this.tabSize = tabSize;
	}

}
