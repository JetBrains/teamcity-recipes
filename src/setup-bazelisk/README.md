# jetbrains/setup-bazelisk

Downloads and installs bazelisk on the agent from [bazelisk github releases](https://api.github.com/repos/bazelbuild/bazelisk/releases).
The path to the installed binary is exposed as the `bazelisk_path` environment variable for downstream steps.

## Inputs

### `input_version`
The bazelisk version in the “major”, “major.minor”, or “major.minor.patch” format. Examples: “1”, “1.26”, “1.26.0”.

### `input_installation_path`
The installation path for the bazelisk on the agent machine. 
Accepts absolute and relative to the agent checkout directory paths. 
By default, the agent’s tools directory is used.