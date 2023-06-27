package org.vgcpge.copilot.ls;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;

public final class CommandStreams {
	private final IOStreams streams;
	
	public IOStreams streams() {
		return streams;
	}
	
	public CommandStreams(List<String> command) throws IOException {
		Process process = new ProcessBuilder(command).redirectError(Redirect.INHERIT).start();
		@SuppressWarnings("resource")
		IOStreams wrapper = new IOStreams(process.getInputStream(), process.getOutputStream());
		streams = IOStreams.whenClosed(wrapper, process::destroy);
	}
}
