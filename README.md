# Text Typer

A compact Java Swing quick panel that enters text into the currently focused application by sending individual keyboard press and release events. It does not use the clipboard to send text (you can paste text into its input box).

## Requirements

- A graphical desktop session (the app cannot run in a headless terminal or server).
- JDK 17 or later. A JDK includes both `java` (to run the app) and `javac` (to compile it).

## Install a JDK

### Ubuntu

Open Terminal and install OpenJDK 17:

```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

Confirm both the runtime and compiler are available:

```bash
java -version
javac -version
```

Both commands should report version 17 or newer. Ubuntu's package manager configures the Java commands automatically; no `JAVA_HOME` setup is needed for these run steps.

### Windows

Install a JDK 17 or newer distribution, such as Eclipse Temurin, using its Windows installer. In the installer, enable the options to add Java to `PATH` (and set `JAVA_HOME` if offered). Open a new PowerShell window and check:

```powershell
java -version
javac -version
```

If either command is not found, reopen the terminal after installation or check that the JDK `bin` folder is on `PATH`.

## Compile and run on Ubuntu

In Terminal, change to the folder containing this README, then compile and launch:

```bash
cd /path/to/Typer
mkdir -p out
javac -d out src/main/java/typer/TextTyperApp.java
java -cp out typer.TextTyperApp
```

Replace `/path/to/Typer` with the actual location of the project. Keep the app running in a logged-in desktop session. Keyboard event injection can be restricted in Wayland sessions; if typing into other apps does not work, log into an Xorg session and launch the app there.

## Compile and run on Windows

From PowerShell in the folder containing this README:

```powershell
New-Item -ItemType Directory -Force out
javac -d out src\main\java\typer\TextTyperApp.java
java -cp out typer.TextTyperApp
```

## Use

Paste or enter text in the panel, set the typing speed, tab width, and start countdown, then select **Start typing**. The countdown is configurable from 0 to 30 seconds (default 4); use 0 to begin immediately. A small floating **STOP TYPING** button appears while text is being entered; click it to stop immediately. The panel's **Cancel** button also cancels the countdown or typing. Tabs in the input are expanded to the selected number of spaces. If the destination editor auto-indents, enable **Let editor auto-indent**; Text Typer then skips the input's leading spaces after each newline and lets the editor add them. Leave it off to type the input's spaces explicitly (for example, in plain text fields or editors with auto-indent disabled). The app does not send Backspace to guess at or remove editor indentation. Spaces and newlines are simulated as keyboard events. The standard keyboard layout characters are supported; other characters are reported before typing begins.
