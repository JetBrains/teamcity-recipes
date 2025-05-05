#!/usr/bin/env bash
set -euo pipefail

command="$input_command"

# Configure AWS credentials if provided
if [[ -n "$input_aws_access_key_id" ]]; then
  aws configure set aws_access_key_id "$input_aws_access_key_id"
fi
if [[ -n "$input_aws_secret_access_key" ]]; then
  aws configure set aws_secret_access_key "$input_aws_secret_access_key"
fi
if [[ -n "$input_aws_region" ]]; then
  aws configure set region "$input_aws_region"
fi

# Execute the AWS CLI command
eval "$command"