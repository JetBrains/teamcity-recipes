# jetbrains/pin-build

Pins the specific build to prevent it from being removed during a scheduled clean-up.
Can only pin a finished build.
This recipe utilizes TeamCity REST API and requires Token-Based Authentication credentials.

## Inputs

### `input_build_id`
The ID of a finished build.

### `input_comment`
The optional pin comment.

### `input_server_url`
The full TeamCity server URL (for example, https://teamcity.jetbrains.com). 
The default value is %teamcity.serverUrl%.

### `input_access_token`
TeamCity user access token (“User Profile | Access Tokens” in TeamCity UI). 
Supports remote parameters that retrieve secrets from external vaults (for example, HashiCorp Vault) and Kotlin DSL Secure value tokens.
