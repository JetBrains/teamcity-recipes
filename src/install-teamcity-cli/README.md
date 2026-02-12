# jetbrains/install-teamcity-cli

Installs the [TeamCity CLI](https://github.com/JetBrains/teamcity-cli) on the agent, allowing subsequent build steps to call the "tc" command.

## Inputs

### `input_version`
The TeamCity CLI version in "major.minor.patch" format, or "latest" to install the latest version available. Examples: "latest", "0.4.0".

### `input_installation_path`
The installation path for the TeamCity CLI on the agent machine.
Accepts absolute and relative to the agent checkout directory paths.
By default, the agent's tools directory is used.
