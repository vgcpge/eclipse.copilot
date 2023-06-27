package org.vgcpge.copilot.ls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

import com.google.common.base.Joiner;

public class CopilotLocator {
	private static final String NO_COPILOT_TEMPLATE = "Copilot is not installed. Install Github Copilot for Nvim: https://docs.github.com/en/copilot/getting-started-with-github-copilot?tool=neovim\nTested locations:\n%s";
	private static final String NO_NODE_TEMPLATE = "Can't locate Node.js. Configure PATH.\nTested locations:\n%s";
	private static final Path NVIM_RELATIVE_PATH = Path.of("nvim", "pack", "github", "start", "copilot.vim", "copilot",
			"dist", "agent.js");
	private static final List<Path> NODE_PATH_CANDIDATES = List.of(Paths.get("/opt/homebrew/bin/node"));

	public CopilotLocator() {
		super();
	}

	public static List<String> copilotStartCommand() {
		return List.of(findNode(), findAgent().toString());
	}
	
	public static IOStreams start(Consumer<String> log) throws IOException {
		List<String> command = CopilotLocator.copilotStartCommand();
		String publicCommand = command.stream().map(CopilotLocator::privacyFilter).collect(Collectors.joining(" "));
		log.accept("Starting: " + publicCommand);
		return new CommandStreams(command).streams();
	}

	private static String findNode() {
		List<String> testedLocations = new ArrayList<>();
		try {

			try {
				String simpleCommand = "node";
				testedLocations.add(simpleCommand);
				if (isValidNode(simpleCommand)) {
					return simpleCommand;
				}
			} catch (IOException e) {
				// No PATH
			}
			for (Path path : NODE_PATH_CANDIDATES) {
				testedLocations.add(privacyFilter(path.toString()));
				if (Files.isExecutable(path)) {
					try {
						if (isValidNode(path.toString())) {
							return path.toString();
						}
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			}
			throw new IllegalStateException(String.format(NO_NODE_TEMPLATE, Joiner.on("\n").join(testedLocations)));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private static boolean isValidNode(String nodeCommand) throws InterruptedException, IOException {
		ProcessBuilder builder = new ProcessBuilder(nodeCommand, "--version");

		Process process = builder.start();
		try {
			return process.waitFor() == 0;
		} finally {
			process.destroyForcibly();
		}
	}

	private static Path findAgent() {
		List<String> testedLocations = new ArrayList<>();
		return configurationLocations().stream() //
				.map(location -> location.resolve(NVIM_RELATIVE_PATH)) //
				.peek(path -> testedLocations.add(privacyFilter(path.toString()))) //
				.filter(Files::isRegularFile) //
				.filter(Files::isReadable) //
				.findFirst() //
				.orElseThrow(() -> new IllegalStateException(
						String.format(NO_COPILOT_TEMPLATE, Joiner.on("\n").join(testedLocations))));
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

	public static String privacyFilter(String data) {
		String home = System.getProperty("user.home");
		if (home != null) {
			data = data.replace(home, "${HOME}");
		}
		return data;
	}
}
