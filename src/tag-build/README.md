# jetbrains/tag-build

Tags a specific TeamCity build. Locates the build to tag by the given build ID.
To tag the current build instead, use the 'tag-current-build' recipe.
This recipe utilizes TeamCity REST API and requires Token-Based Authentication credentials.

## Inputs

### `input_build_id`
The ID of a build that will be tagged.

### `input_tags`
A newline-delimited list of tags to add.

### `input_server_url`
The full TeamCity server URL (for example, https://teamcity.jetbrains.com). 
The default value is %teamcity.serverUrl%.

### `input_access_token`
TeamCity user access token (“User Profile | Access Tokens” in TeamCity UI). 
Supports remote parameters that retrieve secrets from external vaults (for example, HashiCorp Vault) and Kotlin DSL Secure value tokens.
