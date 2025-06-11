# jetbrains/dagger

Installs the Dagger CLI and then optionally runs a Dagger command.
The paths to the installed binary and its directory are exposed as the `DAGGER_EXEC`and `DAGGER_PATH` environment variables for use in downstream steps.

## Inputs

### `input_version`

The version of the Dagger CLI to install, e.g. `0.18.2`.

### `input_command`

The optional Dagger CLI command to execute, e.g. `dagger call build`.

### `input_workdir`

The optional path in which to execute the Dagger command. Can be relative to the agent working directory.

### `input_installation_path`

Directory where the Dagger CLI should be installed.

### `input_stop_engine`

A flag that specifies whether the Dagger engine should be stopped at the end of the build step.

### `input_cloud_token`

The optional Dagger Cloud authentication token.