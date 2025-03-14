# jetbrains/send-slack-message

Sends a Slack notification.
Requires a project that has a configured Slack connection with Notifications limit greater than 0.

## Inputs

### `input_connection_id`
The ID of a Slack connection owned by this project or one of its parent projects.

### `input_send_to`
The Slack channel ID can be copied from the channel `About` tab. A user ID can be retrieved in the `Profile | More Actions | Copy member ID`.

### `input_message`
The message to send. Supports Markdown formatting.
