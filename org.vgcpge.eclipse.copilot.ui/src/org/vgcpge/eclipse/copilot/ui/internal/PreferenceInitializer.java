package org.vgcpge.eclipse.copilot.ui.internal;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;
import org.osgi.framework.FrameworkUtil;
import org.vgcpge.copilot.ls.CopilotLocator;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	private static final ILog LOG = Platform.getLog(PreferenceInitializer.class);
	private static final String PLUGIN_ID = FrameworkUtil.getBundle(PreferenceInitializer.class).getSymbolicName();

	public PreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferenceStore = Configuration.preferenceStore();
		CopilotLocator locator = new CopilotLocator(LOG::warn);
		Location location = Platform.getConfigurationLocation();
		if (location != null) {
			URL configURL = location.getURL();
			if (configURL != null && configURL.getProtocol().equals("file")) { //$NON-NLS-1$
				Path target = Path.of(configURL.getFile(), PLUGIN_ID);
				try {
					Files.createDirectories(target);
				} catch (IOException e) {
					throw new RuntimeException("Failed to create " + target, e);
				}
				locator.setPersistentStorageLocation(target);
			}
		}
		availableNodeJsExcutables(locator).map(Path::toString).findFirst().ifPresent(detected -> {
			preferenceStore.setDefault(Configuration.NODE_JS_EXECUTABLE_KEY, detected);
		});
		locator.availableAgents().map(Path::toString).findFirst().ifPresent(detected -> {
			preferenceStore.setDefault(Configuration.AGENT_JS_KEY, detected);
		});
	}

	public Stream<Path> availableNodeJsExcutables(CopilotLocator locator) {
		Stream<Path> fromLib = locator.availableNodeExecutables();
		// Lazy, as Wild Web produces a side effect when called - unpacks embedded instance
		Stream<Path> fromWildWeb = CopilotLocator.lazy(PreferenceInitializer::findWildWebNodeJs);
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
