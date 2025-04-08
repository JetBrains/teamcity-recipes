# TeamCity Recipes

Recipes are custom build steps based on one or multiple standard TeamCity steps. 
In this repository you can find ready-to-use public [YAML](yaml-specification/TeamCity%20Recipes%20YAML%20format%20specification.md) recipes published to [JetBrains Marketplace](https://plugins.jetbrains.com/teamcity_recipe).

## Use a recipe
Recipes are custom build steps, and as such, are added to build configurations in the same manner.

1. Open configuration settings and navigate to the Build steps settings tab.
2. Click the Add build step button.
3. Choose a recipe from the right column that shows:
   - local recipes owned by this project or its parent project;
   - public recipes from JetBrains Marketplace.
     ![img.png](assets/add-build-step.png)
4. Set up required recipe settings in the same manner you do this for regular TeamCity steps.

## Recipes catalog

### [add-to-path](src/add-to-path/README.md) (Service message)
Temporarily prepends a specified directory to the PATH environment variable during the build process, ensuring its executables take precedence.

### [build-godot-game](src/build-godot-game/README.md) (Bash script with custom Docker image)
Recipe example based on article content from [JetBrains Blog](https://blog.jetbrains.com/teamcity/2024/10/automating-godot-game-builds-with-teamcity/).

### [create-file](src/create-file/README.md) (Kotlin Script)
Creates a text file with the specified content at the given path.
The directory is created automatically if it does not exist.

### [download-file](src/download-file/README.md) (Kotlin Script)
Downloads a file from the specified URL to the given directory.
Supports bearer authentication, as well as custom headers.

### [download-from-s3](src/aws/download-from-s3/README.md) (Kotlin Script)
Retrieves an object from AWS S3 and saves it to a specified local destination.
Allows you to download both individual files and AWS S3 folders.

### [install-aws-cli](src/aws/install-aws-cli/README.md) (Kotlin Script)
Installs the AWS Command Line Interface (CLI) on the agent, allowing subsequent build steps to call the “aws” command.

### [import-xml-report](src/import-xml-report/README.md) (Service Message)
Imports an XML test report.
Supports JUnit, Surefire, NUnit, and VSTest formats.

### [pin-build](src/pin-build/README.md) (REST API)
Pins the specific build to prevent it from being removed during a scheduled clean-up.
Can only pin a finished build.
This recipe utilizes TeamCity REST API and requires Token-Based Authentication credentials.

### [publish-artifacts](src/publish-artifacts/README.md) (Service message)
Publishes build artifacts immediately after they are built, while the build is still running.

### [send-email](src/send-email/README.md) (Service message)
Sends a custom email from a build step.
Requires a configured SMTP server (Admin | Email Notifier in TeamCity UI) with the “Notifications limit” property greater than 0.

### [send-slack-message](src/send-slack-message/README.md) (Service message)
Sends a Slack notification.
Requires a project that has a configured Slack connection with Notifications limit greater than 0.

### [set-build-status](src/set-build-status/README.md) (Service message)
Sets the build status to successful or failed. The status persists after the build completes.

### [set-environment-variable](src/set-environment-variable/README.md) (Service message)
Sets an **environment variable**.  
If the variable does not exist in the environment, it will be **created**.

### [setup-node](src/setup-node/README.md)
Installs Node.js on the agent, allowing subsequent build steps to call the “node” command.

### [tag-current-build](src/tag-current-build/README.md) / [untag-current-build](src/untag-current-build/README.md) (Service message)
Tags the current build.
If you need to tag specific builds by their IDs, use the 'tag-build' recipe instead.

### [tag-build](src/tag-build/README.md) (REST API)
Tags a specific TeamCity build. Locates the build to tag by the given build ID.
To tag the current build instead, use the 'tag-current-build' recipe.
This recipe utilizes TeamCity REST API and requires Token-Based Authentication credentials.

### [upload-to-s3](src/aws/upload-to-s3/README.md) (Kotlin Script)
Uploads a local file or directory to the AWS S3 bucket.
If a directory is selected, the recipe will upload its contents directly to the target path
without creating a corresponding directory in S3.

## Dev quickstart
To build recipes run:
```bash
./gradlew recipes
