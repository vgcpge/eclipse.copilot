package org.vgcpge.eclipse.copilot.ui.internal;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * 
 * { "doc": {
 * "position": { "line": 8, "character": 0 }, "insertSpaces": true, "tabSize":
 * 4, "uri": "file:///private/tmp/python_debugg/testing_python.py", "version": 0
 * } }
 * 
 */
public class CompletionParams {
	  /**
	   * The text document.
	   */
	  @NonNull
	  private TextDocumentPositionParams doc;
	  
	  public CompletionParams(TextDocumentPositionParams doc) {
		  this.doc = Objects.requireNonNull(doc);
	  }
	  
}
