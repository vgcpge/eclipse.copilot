# Unofficial Github Copilot integration with Eclipse

![Scanner completion](images/scanner_completion.png)

# Warning
This integration is an unofficial prototype. Consider trying Copilot with [officially supported tools](https://docs.github.com/en/copilot/getting-started-with-github-copilot).

# Usage
- Download an archive with update site.
- Follow [instructions](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.user/tasks/tasks-124.htm) to install the Copilot feature from the update site.
- A warning dialog "Trust" will request a confirmation to install unsigned content. Install on your own risk and consider gifting me a code-signing certificate.
- Find out what web browser you are using by default in Preferences/General/Web Browser
- Open your default web browser
- [Sign in Github](https://github.com/login)
- When editing a text file for the first time, plug-in will request permission to access Github Copilot in your default web browser. An error will be shown if you don't have Copilot subscription.
- A modal dialog will be shown with a code to supply in the browser. Once granted, permission is remembered by Copilot, this is a one-time operation.

# Java
To work with Java Development Tools (JDT), install additionally "JDT Integration for LSP4E" from the main Eclipse update site.
You may want to remove less useful completion assistants from Preferences/Java/Editor/Content Assist/Advanced. Leave "Language Server Protocols" enabled.
