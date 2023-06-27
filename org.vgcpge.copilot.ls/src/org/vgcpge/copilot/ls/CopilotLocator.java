package org.vgcpge.copilot.ls;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;

public class CopilotLocator {
	private static final String NO_COPILOT_TEMPLATE = "Copilot is not installed. Install Github Copilot for Nvim: https://docs.github.com/en/copilot/getting-started-with-github-copilot?tool=neovim\nTested locations:\n%s";
	private static final String NO_NODE_TEMPLATE = "Can't locate Node.js. Configure PATH.\nTested locations:\n%s";
	private static final Path NVIM_RELATIVE_PATH = Path.of("nvim", "pack", "github", "start", "copilot.vim", "copilot",
			"dist", "agent.js");
	private static final List<Path> NODE_PATH_CANDIDATES = List.of(Paths.get("/opt/homebrew/bin/node"));

	private final Consumer<String> log;
	private Path nodeLocation = null;
	public CopilotLocator(Consumer<String> log) {
		super();
		this.log = Objects.requireNonNull(log);
	}

	public List<String> copilotStartCommand() {
		return List.of(findNode(), findAgent().toString());
	}
	
	public IOStreams start() throws IOException {
		List<String> command = copilotStartCommand();
		String publicCommand = command.stream().map(CopilotLocator::privacyFilter).collect(Collectors.joining(" "));
		log.accept("Starting: " + publicCommand);
		return new CommandStreams(command).streams();
	}
	
	public void setNodeJs(String location) {
		Path path = Paths.get(location);
		if (!Files.isExecutable(path)) {
			throw new IllegalArgumentException(location + " is not executable");
		}
		try {
			if (!isValidNode(path.toString())) {
				throw new IllegalArgumentException(location + " is not a valid Node.js executable");
			}
		} catch (Exception e ) { 
			throw new IllegalArgumentException(location + " is not a valid Node.js executable", e);
		
		}
		nodeLocation = path;
	}


	private String findNode() {
		if (nodeLocation != null) {
			return nodeLocation.toString();
		}
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

	private boolean isValidNode(String nodeCommand) throws InterruptedException, IOException {
		ProcessBuilder builder = new ProcessBuilder(nodeCommand, "--version");
		builder.redirectError(Redirect.DISCARD);

		Process process = builder.start();
		process.getOutputStream().close();
		String version;
		try (InputStream s = process.getInputStream()) {
			version = readAll(s).trim();
		}
		try {
			boolean result = process.waitFor() == 0;
			if (result) {
				log.accept("Node.js location: " + privacyFilter(nodeCommand) + ". Version: " + version);
			}
			return result;
		} finally {
			process.destroyForcibly();
		}
	}
	
	private static String readAll(InputStream input) { 
		try (Scanner s = new Scanner(input, StandardCharsets.UTF_8)) {
			s.useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
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
