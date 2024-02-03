[![Tycho Build](https://github.com/vgcpge/eclipse.copilot/actions/workflows/maven.yml/badge.svg)](https://github.com/vgcpge/eclipse.copilot/actions/workflows/maven.yml)
# Unofficial GitHub Copilot integration with Eclipse

![Scanner completion](images/scanner_completion.png)

# Features
- Content-assist completions

# Warning
This integration is unofficial. Consider trying Copilot with [officially supported tools](https://docs.github.com/en/copilot/getting-started-with-github-copilot).
Copilot is a cloud-based service provided by GitHub and only works with [a paid subscription](https://github.com/settings/copilot). 

# Alternatives
Closed-source [Copilot4Eclipse](https://marketplace.eclipse.org/content/copilot4eclipse) by [Genuitec](https://www.genuitec.com/products/copilot4eclipse/)


# Prerequisites
- [Eclipse 2022-12 (4.26) or newer](https://www.eclipse.org/downloads/), or [its update-site](https://download.eclipse.org/releases/2023-12/)
- [Node.js 19 or newer](https://nodejs.org/en/download/current)

# Usage
See [installation video demo](https://youtu.be/B_QZao3abBw) or follow steps:
- Copy the link to the update site: https://vgcpge.github.io/eclipse.copilot/
- Follow the [instructions](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.user/tasks/tasks-124.htm) to install the Copilot feature from the update site.
- Warning dialogs "Trust Authorities" and "Trust Artifacts" will request confirmations. Install at your own risk and consider gifting me a code-signing certificate.
- If necessary, configure Preferences/Language Servers/Copilot/Node.js executable
- Hit <kbd>Ctrl+Space</kbd> for [content-assist](https://www.tutorialspoint.com/eclipse/eclipse_content_assist.htm) in the ["Generic Text Editor"](https://projects.eclipse.org/projects/technology.tm4e) or the ["Java Editor"](https://www.eclipse.org/jdt/) (requires [Java support](#java) to be installed separately). Any other editor supporting the standard APIs will work too (notably, "Text Editor" does not support content-assist).
- When editing a text file (any source code file counts) for the first time, plug-in will request permission to access GitHub Copilot in your default web browser. An error will be shown if you don't have Copilot subscription.
- A modal dialog will be shown with a code to supply in the browser. Once granted, permission is remembered by Copilot, this is a one-time operation.

# Java
To work with [Java Development Tools (JDT)](https://www.eclipse.org/jdt/), install additionally "JDT Integration for LSP4E" from the main Eclipse update site.
You may want to remove less useful completion assistants from Preferences/Java/Editor/Content Assist/Advanced. Leave "Language Server Proposals" enabled.

# Credits
- This integration relies on [Eclipse LSP4E - Language Server Protocol client for Eclipse IDE](https://github.com/eclipse/lsp4e)
- Inspiration for this project comes from the first attempt by [masecla22](https://github.com/masecla22/eclipse-github-copilot-integration). No code has been reused from that project here.
