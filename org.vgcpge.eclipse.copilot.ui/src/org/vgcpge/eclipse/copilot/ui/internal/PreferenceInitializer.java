package org.vgcpge.eclipse.copilot.ui.internal;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;
import org.vgcpge.copilot.ls.CopilotLocator;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	private static final ILog LOG = Platform.getLog(PreferenceInitializer.class);

	public PreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferenceStore = Configuration.preferenceStore();
		CopilotLocator locator = new CopilotLocator(LOG::warn);
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
		Stream<Path> fromWildWeb = lazy(PreferenceInitializer::findWildWebNodeJs);
		return Stream.concat(fromLib, fromWildWeb);
	}

	public static <T> Stream<T> lazy(Supplier<Stream<T>> supplier) {
		return Stream.generate(supplier).limit(1).flatMap(Function.identity());
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
