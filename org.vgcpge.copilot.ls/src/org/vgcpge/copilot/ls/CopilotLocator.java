package org.vgcpge.copilot.ls;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.base.Joiner;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

public class CopilotLocator {
	private static final String NO_COPILOT_TEMPLATE = "Copilot is not installed. Install Github Copilot for Nvim: https://docs.github.com/en/copilot/getting-started-with-github-copilot?tool=neovim\nTested locations:\n%s";
	private static final String NO_NODE_TEMPLATE = "Can't locate Node.js. Configure PATH.\nTested locations:\n%s";
	private static final List<Path> NVIM_RELATIVE_PATHS = List.of(//
			Path.of("nvim", "pack", "github", "start", "copilot.vim", "copilot", "dist", "agent.js"), //
			Path.of("nvim", "pack", "github", "start", "copilot.vim", "dist", "agent.js"),
			Path.of(".vim", "pack", "github", "start", "copilot.vim", "dist", "agent.js"),
			Path.of(".vim", "pack", "github", "start", "copilot.vim", "copilot", "dist", "agent.js"));
	private static final List<Path> NODE_PATH_CANDIDATES = List.of(Paths.get("/opt/homebrew/bin/node"));

	private final Consumer<String> log;
	private Path nodeLocation = null;
	private Path agentLocation = null;

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

	/** Force use of a given Node.js executable. Disable autodetect. */
	public void setNodeJs(String location) {
		Path path = Paths.get(location);
		if (!Files.isExecutable(path)) {
			throw new IllegalArgumentException(location + " is not executable");
		}
		try {
			if (!isValidNode(path.toString())) {
				throw new IllegalArgumentException(location + " is not a valid Node.js executable");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(location + " is not a valid Node.js executable", e);

		}
		nodeLocation = path;
	}

	/**
	 * Force use of a given Copilot agent. Disable autodetect.
	 * 
	 * @param agentJsPath - a path to JavaScript entrypoint. Usually named agent.js.
	 */
	public void setAgentJs(String agentJsPath) {
		Path path = Paths.get(agentJsPath);
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException(agentJsPath + " is not a file");
		}
		if (!agentJsPath.endsWith(".js")) {
			throw new IllegalArgumentException(agentJsPath + " is not a JavaScript file");
		}
		if (!Files.isReadable(path)) {
			throw new IllegalArgumentException(agentJsPath + " is not readable");
		}
		agentLocation = path;
	}

	private final List<String> testedAgentLocations = new ArrayList<>();
	private Path persistentStorage = null;

	public static <T> Stream<T> lazy(Supplier<Stream<T>> supplier) {
		return Stream.generate(supplier).limit(1).flatMap(Function.identity());
	}

	public Stream<Path> availableAgents() {
		testedAgentLocations.clear();
		Stream<Path> externalLocations = configurationLocations().stream() //
		.flatMap(location -> NVIM_RELATIVE_PATHS.stream().map(relative -> location.resolve(relative)));
		Stream<Path> downloadedAgent = lazy(() -> downloadAgent().stream());
		return Stream.concat(externalLocations, downloadedAgent) //
				.peek(path -> testedAgentLocations.add(privacyFilter(path.toString()))) //
				.filter(Files::isRegularFile) //
				.filter(Files::isReadable);
	}

	private Optional<Path> downloadAgent() {
		if (persistentStorage == null) {
			return Optional.empty();
		}
		URL downloadURI;
		try {
			downloadURI = new URL("https://codeload.github.com/github/copilot.vim/zip/refs/heads/release");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		Path destination = persistentStorage.resolve("downloaded-agent");
		Path result = destination.resolve("copilot.vim-release").resolve("dist").resolve("agent.js");
		if (Files.exists(result)) {
			return Optional.of(result);
		}
		
		log.accept("Downloading agent to " + privacyFilter(destination.toString()));
		try {
			if (Files.exists(destination)) {
				MoreFiles.deleteRecursively(destination, RecursiveDeleteOption.ALLOW_INSECURE);
			}
			Path tempFile;
			try {
				tempFile = Files.createTempFile("copilot-agent", ".zip");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			try (InputStream in = downloadURI.openStream()) {
				try (ReadableByteChannel rbc = Channels.newChannel(in);
						FileOutputStream fos = new FileOutputStream(tempFile.toFile());
						FileChannel channel = fos.getChannel()) {
					channel.transferFrom(rbc, 0, Long.MAX_VALUE);
				}
				// unzip
				try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(tempFile))) {
					ZipEntry entry = zipIn.getNextEntry();
					while (entry != null) {
						Path filePath = destination.resolve(entry.getName());
						if (!entry.isDirectory()) {
							Files.createDirectories(filePath.getParent());
							Files.copy(zipIn, filePath);
						}
						zipIn.closeEntry();
						entry = zipIn.getNextEntry();
					}
				}
			} finally {
				Files.deleteIfExists(tempFile);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (!Files.exists(result)) {
			throw new RuntimeException("Archive downloaded from " + downloadURI + " is missing agent.js file");
		}
		return Optional.of(result);
	}

	public Stream<Path> availableNodeExecutables() {
		return commandCandidates().map(this::nodeCommandToPath).flatMap(Optional::stream);
	}

	/** A location to cache useful files. For example - agent download. */
	public void setPersistentStorageLocation(Path path) {
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException(path + " is not a directory");
		}
		if (!Files.isWritable(path)) {
			throw new IllegalArgumentException(path + " is not writable");
		}
		this.persistentStorage = Objects.requireNonNull(path);
	}

	private Stream<String> commandCandidates() {
		return Stream.concat(Stream.of("node"), NODE_PATH_CANDIDATES.stream()).map(Object::toString);
	}

	private String findNode() {
		if (nodeLocation != null) {
			return nodeLocation.toString();
		}
		return availableNodeExecutables().findFirst().map(Path::toString).orElseThrow(() -> new IllegalStateException(
				String.format(NO_NODE_TEMPLATE, commandCandidates().collect(Collectors.joining("\n")))));
	}

	private boolean isValidNode(String nodeCommand) throws InterruptedException, IOException {
		Optional<String> result = checkOutput(List.of(nodeCommand, "--version")).filter(Predicate.not(String::isEmpty));
		result.ifPresent(version -> {
			log.accept("Node.js location: " + privacyFilter(nodeCommand) + ". Version: " + version);
		});
		return result.isPresent();
	}

	private static String readAll(InputStream input) {
		try (Scanner s = new Scanner(input, StandardCharsets.UTF_8)) {
			s.useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
	}

	private Path findAgent() {
		if (agentLocation != null) {
			return agentLocation;
		}
		return availableAgents().findFirst() //
				.orElseThrow(() -> new IllegalStateException(
						String.format(NO_COPILOT_TEMPLATE, Joiner.on("\n").join(testedAgentLocations))));
	}

	private static List<Path> configurationLocations() {
		var result = new ArrayList<Path>();
		String home = System.getProperty("user.home");
		result.add(Paths.get(home));
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

	private Optional<Path> nodeCommandToPath(String command) {
		return checkOutput(List.of(command, "--eval", "console.log(process.execPath)")).map(Path::of).filter(path -> {
			if (!Files.isExecutable(path)) {
				log.accept("Not an executable: " + privacyFilter(path.toString()));
				return false;
			}
			return true;
		});
	}

	private Optional<String> checkOutput(List<String> command) {
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectError(Redirect.PIPE);
		Process process;
		try {
			try {
				process = pb.start();
			} catch (IOException e) {
				// Cannot run program "node"
				// There is not such program on PATH
				return Optional.empty();
			}
			try {
				process.getOutputStream().close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			String result;
			try (InputStream inputStream = process.getInputStream()) {
				// Warning: will hang forever if error stream overflows OS buffer
				result = readAll(inputStream).trim();
			}
			String error;
			try (InputStream inputStream = process.getErrorStream()) {
				error = readAll(inputStream).trim();
			}

			if (process.waitFor() != 0) {
				log.accept("Failed to run \""
						+ command.stream().map(CopilotLocator::privacyFilter).collect(Collectors.joining(" ")) + "\":\n"
						+ error);
				return Optional.empty();
			}
			return Optional.of(result);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

}