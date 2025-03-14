# jetbrains/send-email

Sends a custom email from a build step.
Requires a configured SMTP server (Admin | Email Notifier in TeamCity UI) with the “Notifications limit” property greater than 0.

## Inputs

### `input_address`
A comma-separated list of recipient email addresses. These addresses must match the rules specified in the TeamCity Email Notifier’s Allowed addresses setting.

### `input_subject`
The email subject.

### `input_message`
The message body in plain text format. 
URLs to external resources are automatically blocked. 
To include external links, update the “Email Notifier | Allowed Hostnames” parameter.
