package org.vgcpge.eclipse.copilot.ui.internal;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		setPreferenceStore(createPreferenceStore());
	}

	private static IPreferenceStore createPreferenceStore() {
		return Configuration.preferenceStore();
	}

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	protected void createFieldEditors() {
		addField(new ExecutableFieldEditor(Configuration.NODE_JS_EXECUTABLE_KEY, "Node.js executable", getFieldEditorParent()));
		FileFieldEditor agentJsField = new FileFieldEditor(Configuration.AGENT_JS_KEY, "Copilot agent", getFieldEditorParent());
		agentJsField.setFileExtensions(new String[] { "*.js" });
		addField(agentJsField);
	}

}
