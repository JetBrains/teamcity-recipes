# jetbrains/install-aws-cli

Installs the AWS Command Line Interface (CLI) on the agent, allowing subsequent build steps to call the “aws” command.

## Inputs

### `input_aws_cli_version`
The AWS CLI version in 'major.minor.patch' format, or 'latest' to install the latest version available.
Minimum supported version: 2.0.0 (AWS CLI v1 is not supported).

### `input_installation_path`
The installation path for the AWS CLI on the agent machine.
Accepts absolute (/lib/apps/aws-cli, C:\util\aws-cli) and relative to the agent checkout directory (./aws-cli) paths. 
By default, the agent’s tools dir is used.