# jetbrains/set-build-status

Sets the build status to successful or failed. The status persists after the build completes.

## Inputs

### `input_status`
- **Options**:
  - `SUCCESS`
  - `FAILURE`

### `input_text`
The custom text for a new build status. Use the {build.status.text} placeholder for the default status text.