package org.vgcpge.eclipse.copilot.ui.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;
import org.osgi.framework.FrameworkUtil;
import org.vgcpge.copilot.ls.CopilotLocator;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	private static final ILog LOG = Platform.getLog(PreferenceInitializer.class);
	static final String PLUGIN_ID = FrameworkUtil.getBundle(PreferenceInitializer.class).getSymbolicName();

	public PreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferenceStore = Configuration.preferenceStore();
		CopilotLocator locator = new CopilotLocator(LOG::warn);
		locator.setPersistentStorageLocation(Configuration.getPersistentStorage());
		availableNodeJsExcutables(locator).map(Path::toString).findFirst().ifPresent(detected -> {
			preferenceStore.setDefault(Configuration.NODE_JS_EXECUTABLE_KEY, detected);
		});
		locator.availableAgents().map(Path::toString).findFirst().ifPresent(detected -> {
			preferenceStore.setDefault(Configuration.AGENT_JS_KEY, detected);
		});
	}

	public Stream<Path> availableNodeJsExcutables(CopilotLocator locator) {
		Stream<Path> fromLib = locator.availableNodeExecutables();
		// Lazy, as Wild Web produces a side effect when called - unpacks embedded
		// instance
		Stream<Path> fromWildWeb = CopilotLocator.lazy(PreferenceInitializer::findWildWebNodeJs)
				.filter(path -> {
					try {
						String error = locator.validateNode(path.toString());
						if (!error.isEmpty()) {
							LOG.info(error);
							return false;
						}
						return true;
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return false;
					} catch (IOException e) {
						LOG.error("Can't validate Node.js" + path, e);
						return false;
					}
				});
		
		return Stream.concat(fromLib, fromWildWeb);
	}

	public static Stream<Path> findWildWebNodeJs() {
		try {
			return Stream.of(NodeJSManager.getNodeJsLocation().toPath());
		} catch (NoClassDefFoundError e) {
			// NoClassDefFoundError happens when optional dependency is not installed
			return Stream.empty();
		}
	}
}
