[![Tycho Build](https://github.com/vgcpge/eclipse.copilot/actions/workflows/maven.yml/badge.svg)](https://github.com/vgcpge/eclipse.copilot/actions/workflows/maven.yml)
# Unofficial Github Copilot integration with Eclipse

![Scanner completion](images/scanner_completion.png)

# Features
- Content assist completions

# Warning
This integration is unofficial. Consider trying Copilot with [officially supported tools](https://docs.github.com/en/copilot/getting-started-with-github-copilot).
Copilot is a cloud-based service provided by Github and only works with [a paid subscription](https://github.com/settings/copilot). 

# Prerequisites
- [Copilot for Neovim](https://docs.github.com/en/copilot/getting-started-with-github-copilot?tool=neovim) (no need for Neovim itself)
- Node.js [any version except 18](https://docs.github.com/en/copilot/getting-started-with-github-copilot?tool=neovim#prerequisites-3)
- Java 17 or newer
- Eclipse IDE should be configured to use Java 17 or newer

# Usage
- Copy the link to update site: https://vgcpge.github.io/eclipse.copilot/
- Follow [instructions](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.user/tasks/tasks-124.htm) to install the Copilot feature from the update site.
- Warning dialogs "Trust Authorities" and "Trust Artifacts" will request confirmations. Install on your own risk and consider gifting me a code-signing certificate.
- Hit <kbd>Ctrl+Space</kbd> for [content-assist](https://www.tutorialspoint.com/eclipse/eclipse_content_assist.htm) in ["Generic Text Editor"](https://projects.eclipse.org/projects/technology.tm4e) or ["Java Editor"](https://www.eclipse.org/jdt/) (requires [Java support](#java) to be installed separately). Any other editor suporting the standard APIs will work too (notably, "Text Editor" does not support content-assist).
- When editing a text file (any source code file counts) for the first time, plug-in will request permission to access Github Copilot in your default web browser. An error will be shown if you don't have Copilot subscription.
- A modal dialog will be shown with a code to supply in the browser. Once granted, permission is remembered by Copilot, this is a one-time operation.

# Java
To work with [Java Development Tools (JDT)](https://www.eclipse.org/jdt/), install additionally "JDT Integration for LSP4E" from the main Eclipse update site.
You may want to remove less useful completion assistants from Preferences/Java/Editor/Content Assist/Advanced. Leave "Language Server Proposals" enabled.

# MacOS
There is no convenient way to configure environment variables for GUI apps on MacOS. For this reason the plugin looks for Node.js executable in /opt/homebrew/bin/node.
If your Node.js is installed elsewhere, run Eclipse from terminal with PATH preconfigured.

# Credits
- This integration relies on [Eclipse LSP4E - Language Server Protocol client for Eclipse IDE](https://github.com/eclipse/lsp4e)
- Inspriration for this project comes from the first attempt by [masecla22](https://github.com/masecla22/eclipse-github-copilot-integration). No code have been reused from that project here.
