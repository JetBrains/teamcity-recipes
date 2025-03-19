# jetbrains/download-from-s3

Retrieves an object from AWS S3 and saves it to a specified local destination.
Allows you to download both individual files and AWS S3 folders.

## Inputs

### `input_source`
The S3 path of the object to download (e.g. s3://bucket/path/to/file).

### `input_target`
The local file or directory path where the downloaded object will be stored.
Accepts absolute (/path/to/file, C:\path\to\file) and relative to the agent working directory (path/to/file) paths.
