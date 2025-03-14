# jetbrains/publish-artifacts

Publishes build artifacts immediately after they are built, while the build is still running.

## Inputs

### `input_path_to_publish`
Newline- or comma-separated paths in the same format as in build configuration’s Artifact paths property. 
Accepts absolute and relative to the build checkout directory paths, and allows you to use asterisk (*) as a wildcard.
