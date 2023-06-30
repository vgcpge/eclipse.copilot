package org.vgcpge.eclipse.copilot.ui.internal;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.widgets.Composite;

public class ExecutableFieldEditor extends FileFieldEditor {
	public ExecutableFieldEditor(String name, String label, Composite fieldEditorParent) {
		super(name, label, true, fieldEditorParent);
	}

	@Override
	protected boolean doCheckState() {
		boolean result = super.doCheckState();
		if (!result)
			return result;
		Path path = Path.of(getTextControl().getText());
		if (!Files.isExecutable(path)) {
			setErrorMessage("File is not executable");
			return false;
		}
		return true;
	}
}
