#!/bin/python3

from subprocess import check_output, check_call
from re import compile
from sys import argv


def current_version():
	"""@returns: a version of Maven project in the current directory like: 0.0.10-SNAPSHOT"""
	# Command to run
	cmd = ['mvn', '--non-recursive', 'help:evaluate', '-Dexpression=project.version', '-q', '-DforceStdout']

	# Run the command and get the output: 0.0.10-SNAPSHOT
	output = check_output(cmd, text=True)

	# The output may contain a trailing newline
	return output.rstrip("\n")

def set_version(version):
	"""
		Sets the version of Maven project in the current directory
	    @param version: a version like: 0.0.10-SNAPSHOT
	"""
	# Command to run
	cmd = ['mvn', 'org.eclipse.tycho:tycho-versions-plugin:set-version', '-Dtycho.mode=maven', f"-DnewVersion={version}"]

	# Run the command
	check_call(cmd)

def bump_patch_version(version):
	"""
		@param version: a version like: 0.0.10-SNAPSHOT
		@returns: a new version with bumped patch version like: 0.0.11-SNAPSHOT
	"""
	# Compile a pattern to match a version string like: 0.0.10-SNAPSHOT
	pattern = compile(r"(\d+)\.(\d+)\.(\d+)(-.*)?")

	# Match the pattern against the version string
	match = pattern.match(version)

	# If the version string does not match the pattern, we raise an exception
	if not match:
		raise ValueError(f"Invalid version string: {version}")

	# Get the major, minor and patch version numbers
	major, minor, patch, qualifier = match.groups()
	
	patch = int(patch) + 1

	# Return the new version string
	return f"{major}.{minor}.{patch}{qualifier}"

def bump():
	# Get the current version
	version = current_version()

	# Print the old version
	print("Current version", version)

	# Bump the patch version
	new_version = bump_patch_version(version)

	# Set the new version
	set_version(new_version)

	# Print the new version
	print("New version", new_version)

command = {'bump': bump, 'current_version': lambda: print(current_version().replace('-SNAPSHOT', ''))}

if __name__ == "__main__":
	command[argv[1]]()