# jetbrains/upload-to-s3

Uploads a local file or directory to the AWS S3 bucket.
If a directory is selected, TeamCity will upload its contents directly to the target path
without creating a corresponding directory in S3.

## Inputs

### `input_source`
The local file or directory path to be uploaded.
Accepts absolute (/path/to/file, C:\path\to\file) and relative to the agent working directory (path/to/file) paths.

### `input_target`
The destination S3 path (e.g. s3://bucket/path/to/file).
