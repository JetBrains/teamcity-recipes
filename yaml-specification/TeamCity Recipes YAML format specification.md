# TeamCity Recipes YAML format specification

## Glossary

* Public recipes – recipes uploaded to the JetBrains Marketplace, publicly available.
* Private recipes – recipes local to a specific TeamCity instance.

## General structure

Example:
```yaml
name: my-org/recipe-name
title: The recipe title
version: 1.2.3
description: The recipe description
container: <container properties>
inputs: <input list>
steps: <step list>
```

Field description:

* **name**: a string value specifying the recipe's name. Mandatory.

  For public recipes the name must be in the ```<namespace>/<name-in-namespace>``` format. Apart from the namespace-name delimiter, it should contain only letters, digits, underscores, and dashes. Each part must not start or end with a dash or underscore, and must not contain several consecutive dashes and underscores.
    * The namespace part designates a namespace on the Marketplace that is unique and belongs to the recipe's vendor. The minimum length is 5 characters, the maximum length is 30 characters.
    * The name-in-namespace part designates the unique name in the namespace. The minimum length is 5 characters, the maximum length is 30 characters.

  For private recipes the name can be either in the ```<namespace>/<name-in-namespace>``` format as specified above, or a single name that does not start or end with a dash or underscore, and does not contain several consecutive dashes and underscores. The maximum length of a single name is 100 characters.
* **title**: a string value specifying the recipe's title visible in the build step selection UI. Mandatory for public recipes. The maximum length for public recipes is 50 characters.
* **version**: a string value specifying the recipe's version in the ```major.minor.patch``` format. The 'minor' and 'patch' parts are optional. Mandatory for public recipes.
* **description**: a string value specifying the description of the recipe. Mandatory. The maximum length for public recipes is 1000 characters.
* **container**: properties that specify whether the recipe will be executed in a container and the details of the execution. Non-mandatory.
* **inputs**: a list of the recipe's inputs. Non-mandatory.
* **steps**: a list of the recipe's steps. Mandatory.

## Input entry

Example:
```yaml
example_input_name:
  type: text
  label: Example input
  description: This is an example input
  default: default input value
  required: true
  <type-specific fields>
```

The key of the YAML object defines the input name. The maximum length of the input name for public recipes is 50 characters.
Field description:

* **type**: the type of the input. Mandatory.
  * The following input types are available:
    * text – the input value can contain any arbitrary text. In the UI, a text control will be displayed for the input.
    * boolean – the input value can be either ```true``` or ```false```. In the UI, a checkbox control will be displayed for the input.
    * select – the input value can be one of the predefined values of the input parameter. In the UI, a dropdown control will be displayed for the input.
    * password – the input value can contain any arbitrary text. The value will be masked with asterisks in both the TeamCity UI and logs. In the UI, a text control will be displayed for the input.
* **label**: a string value specifying the input's label in the UI. The maximum length for public recipes is 100 characters. Non-mandatory.
* **description**: a string value specifying the input's description in the UI. The maximum length for public recipes is 250 characters. Non-mandatory.
* **default**: a string value specifying the default value for the input. The default must be a valid value for the input type. Either the 'default' value must be specified, or 'required' must be set to true. Non-mandatory.
* **required**: a boolean value specifying whether a non-empty value must be provided for the input when referencing the recipe from a TeamCity build configuration or another recipe. Either the 'default' value must be specified, or 'required' must be set to true. Non-mandatory.

To create an environment variable for the input, use the ```env.``` prefix for the input's name, e.g. ```env.message```.
As a general rule, it is better to use environment variables to pass untrusted user input and regular inputs to pass trusted user input and secret values for the following reasons:
* Regular input values are referenced in the script using standard TeamCity parameter reference syntax: ```%param_name%```. Since these values are resolved by text substitution, referencing untrusted user input opens up a possibility for code injection. A common example of untrusted user input for git-triggered builds is git commit message content, committer name, committer email, etc. If there is no untrusted user input, using regular inputs is safe.
* Environment variables are safe to use in scripts to resolve untrusted user input. However, they are best avoided for secret values, as this exposes the secrets to all child processes in the process tree and makes the secrets visible through tools that list process information.

### Type-specific fields

Inputs can have additional fields which are specific to the input type. Currently, only the 'select' type has type-specific fields.

Select input fields example:
```yaml
options:
  - option1
  - option2
```

Field description:

* **options**: list of strings specifying the options of the select input. At least one option must be specified.

## Step format

Script step example:
```yaml
name: Script step
script: echo "hello"
```

Kotlin script step example:
```yaml
name: Kotlin script step
kotlin-script: print("hello")
```

Example of a step with a recipe call:
```yaml
name: Recipe step
uses: jetbrains/some_recipe@1.2.3
inputs:
  input1: foo
  input2: 123
```

Example of a step with container properties:
```yaml
name: Step that runs in a container
container:
  image: alpine
  platform: linux
script: echo "hello"
```

Field description:

* **name**: a string value specifying the step's name. The maximum length for public recipes is 100 characters. Non-mandatory.
* **container**: properties that specify whether the step will be executed in a container and the details of the execution. Non-mandatory.
* **script**: a string value that specifies a command-line script to be executed. The maximum length for public recipes is 50000 characters.
* **kotlin-script**: a string value that specifies a Kotlin script to be executed. The maximum length for public recipes is 50000 characters.
* **uses**: a string value specifying the referenced recipe to be executed. To reference a public recipe, use the format ```<namespace>/<name>@<version>```. Private recipes can reference other private recipes using the format ```private/<recipe-id>```.
* **inputs**: a list of key-value objects that specify the recipe's inputs. To pass the value of the referencing recipe's input into the referenced recipe's input, the input must be referenced as ```referenced-recipe-input-name: %referencing-recipe-input-name%```.

Exactly one of 'script', 'kotlin-script' and 'uses' must be defined in a recipe step. A step cannot have 'script', 'kotlin-script' and 'uses' fields defined simultaneously. If a step uses the ‘script’ or ‘kotlin-script’ field, it must not define an ‘inputs’ field.”

## Container properties format

Example of a short-hand container image reference:
```yaml
container: alpine
```

Example of a full container image reference with additional parameters:
```yaml
container:
  image: alpine
  platform: linux
  parameters: -it
```

Field description:

* **container**: either a string value or a YAML object that defines the container execution parameters.
  * String value – short hand form that defines the container image to be used for the execution.
  * YAML object – full form that allows to define the execution parameters along with the container image.
* **image**: a string value specifying the container image's name. Mandatory in full form.
* **platform**: a string value specifying the container image platform. Can be either 'linux' or 'windows' (case-insensitive). Non-mandatory.
* **parameters**: options for the container run. Non-mandatory.