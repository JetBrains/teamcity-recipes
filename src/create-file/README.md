# jetbrains/create-file

Creates a text file with the specified content at the given path.
The directory is created automatically if it does not exist.

## Inputs

### `input_path`
The path where a new file is created.
Accepts absolute (/var/log/output.txt, C:\logs\output.txt) and relative to the agent working directory (logs/file.txt) paths.

### `input_content`
The contents of a newly created file.