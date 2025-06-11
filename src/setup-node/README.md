# jetbrains/setup-node

Installs Node.js on the agent.
The paths to the installed binary and its directory are exposed as the `NODE_EXEC` and `NODE_PATH` environment variables for use in downstream steps.

## Inputs

### `input_version`
The Node.js version in the “major”, “major.minor”, or “major.minor.patch” format. Examples: “18”, “18.2”, “18.2.0”.

### `input_installation_path`
The installation path for the Node.js on the agent machine. 
Accepts absolute (/lib/apps/node, C:\util\node) and relative to the agent checkout directory (util/node) paths. 
By default, the agent’s tools directory is used.