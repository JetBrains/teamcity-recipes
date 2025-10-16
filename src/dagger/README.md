# jetbrains/dagger

Installs the Dagger CLI and then optionally runs a Dagger command.
The installed CLI can be accessed via the "dagger" command in the subsequent build steps.

## Inputs

### `input_version`

The version of the Dagger CLI to install, e.g. `0.18.2`.

### `input_command`

The optional Dagger CLI command to execute, e.g. `dagger call build`.

### `input_workdir`

The optional path in which to execute the Dagger command. Can be relative to the agent working directory.

### `input_installation_path`

Directory where the Dagger CLI should be installed.

### `input_cloud_token`

The optional Dagger Cloud authentication token.