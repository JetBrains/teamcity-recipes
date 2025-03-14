# jetbrains/download-file

Downloads a file from the specified URL to the given directory.
Supports bearer authentication, as well as custom headers.

## Inputs

### `input_url`
The URL of a file to download.

### `input_output_dir`
The absolute (/var/log/, C:\logs\) or relative to the agent working directory (./logs/) path where the file should be saved. 
Creates the required directory if no such path exists.
Use '.' to save file to the working dir.

### `input_filename`
The name of the saved file. Leave this property empty to retain the original file name.

### `input_bearer_token`
The optional Bearer Token value.

### `input_headers`
The newline-delimited list of headers in the 'Header-Name: Header-Value' format.

