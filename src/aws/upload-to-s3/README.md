# jetbrains/upload-to-s3

Uploads a local file or directory to the AWS S3 bucket.
If a directory is selected, the recipe will upload its contents directly to the target path
without creating a corresponding directory in S3.

AWS credentials and AWS region can be provided in recipe settings.
If credentials are not specified, the default AWS credentials provider chain will be used.
Alternatively, the "Amazon Web Services (AWS)" connection can be configured at the project level and accessed via the "AWS Credentials" build feature.

## Inputs

### `input_source`
The local file or directory path to be uploaded.
Accepts absolute (/path/to/file, C:\path\to\file) and relative to the agent working directory (path/to/file) paths.
The allowed locations are the build working directory, build checkout directory, and build temp directory.

### `input_target`
The destination S3 path (e.g. s3://bucket/path/to/file).

### `input_aws_access_key_id`
The optional AWS account access key ID.

### `input_aws_secret_access_key`
The optional AWS account secret access key.

### `input_aws_region`
The optional AWS region.
