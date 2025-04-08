# jetbrains/download-from-s3

Retrieves an object from AWS S3 and saves it to a specified local destination.
Allows you to download both individual files and AWS S3 folders.

AWS credentials and AWS region can be provided in recipe settings.
If credentials are not specified, the default AWS credentials provider chain will be used.
Alternatively, the "Amazon Web Services (AWS)" connection can be configured at the project level and accessed via the "AWS Credentials" build feature.

## Inputs

### `input_source`
The S3 path of the object to download (e.g. s3://bucket/path/to/file).

### `input_target`
The local file or directory path where the downloaded object will be stored.
Accepts absolute (/path/to/file, C:\path\to\file) and relative to the agent working directory (path/to/file) paths.

### `input_aws_access_key_id`
The optional AWS account access key ID.

### `input_aws_secret_access_key`
The optional AWS account secret access key.

### `input_aws_region`
The optional AWS region.
