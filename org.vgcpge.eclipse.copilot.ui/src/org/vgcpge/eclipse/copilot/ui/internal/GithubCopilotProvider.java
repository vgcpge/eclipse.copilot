package org.vgcpge.eclipse.copilot.ui.internal;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

public class GithubCopilotProvider extends ProcessStreamConnectionProvider {
	private static final Path NVIM_RELATIVE_PATH = Path.of("nvim", "pack", "github", "start", "copilot.vim", "copilot",
			"dist", "agent.js");

	public GithubCopilotProvider() {
		super(Arrays.asList("node", findAgent().toString()),
				Paths.get(URI.create(System.getProperty("osgi.instance.area"))).toString());
	}

	private static Path findAgent() {
		return configurationLocations().stream() //
				.map(location -> location.resolve(NVIM_RELATIVE_PATH)) //
				.filter(Files::isRegularFile) //
				.filter(Files::isReadable) //
				.findFirst() //
				.orElseThrow(() -> new IllegalStateException("Copilot is not installed."));
	}

	private static List<Path> configurationLocations() {
		var result = new ArrayList<Path>();
		String home = System.getProperty("user.home");
		result.add(Paths.get(home).resolve(".config"));
		String data = System.getenv("LOCALAPPDATA");
		if (data != null) {
			result.add(Paths.get(data));
		}
		return result;
	}
}
