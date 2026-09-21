#!/bin/bash

set -euo pipefail

sudo apt-get update && apt-get install jq curl unzip openjdk-17-jdk ffmpeg -y -qq

GITHUB_TOKEN=""
echo $GITHUB_TOKEN > /tmp/token.txt
RUNNER_VERSION="2.317.0"
SDK_VERSION="11076708"
RUNNER_ORG=""
RUNNER_TOKEN=$(curl -s -L -X POST -H "Accept: application/vnd.github+json" -H "Authorization: Bearer $GITHUB_TOKEN" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/$RUNNER_ORG/actions/runners/registration-token | jq -r .token)
USER="ubuntu"

# Install Runner
su - $USER -c "
mkdir -p actions-runner
cd actions-runner
curl -o actions-runner-linux-x64-$RUNNER_VERSION.tar.gz -L https://github.com/actions/runner/releases/download/v$RUNNER_VERSION/actions-runner-linux-x64-$RUNNER_VERSION.tar.gz
tar xzf ./actions-runner-linux-x64-$RUNNER_VERSION.tar.gz"

su - $USER -c "
/home/$USER/actions-runner/config.sh --url https://github.com/$RUNNER_ORG --token $RUNNER_TOKEN --unattended"

# config.sh creates the runner environment files, so customize them afterwards.
su - $USER -c "
cat <<EOF >> ~/actions-runner/.env
ANDROID_HOME=/home/$USER/android
ANDROID_SDK_ROOT=/home/$USER/android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64/
EOF
cat <<EOF >> ~/actions-runner/.path
/home/$USER/android/cmdline-tools/latest/bin
/home/$USER/android/platform-tools
/home/$USER/android/emulator
EOF"

# Install Android SDK
SDK_ARCHIVE="/tmp/commandlinetools-linux-${SDK_VERSION}_latest.zip"
curl --fail --location --retry 5 --retry-all-errors \
  --output "$SDK_ARCHIVE" \
  "https://dl.google.com/android/repository/commandlinetools-linux-${SDK_VERSION}_latest.zip"
install -d -o "$USER" -g "$USER" \
  "/home/$USER/android/cmdline-tools/latest" \
  "/home/$USER/android/tmp"
su - $USER -c "unzip -q '$SDK_ARCHIVE' -d ~/android/tmp/"
su - $USER -c "mv ~/android/tmp/cmdline-tools/* ~/android/cmdline-tools/latest/"
su - $USER -c "
cat <<EOF >> ~/.bashrc
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64/
export ANDROID_HOME=/home/$USER/android/
export ANDROID_SDK_ROOT=/home/$USER/android/
export PATH=/home/$USER/android/cmdline-tools/latest/bin:/home/$USER/android/platform-tools:/home/$USER/android/emulator:\${PATH}
EOF"

test -x /home/$USER/android/cmdline-tools/latest/bin/sdkmanager
/home/$USER/android/cmdline-tools/latest/bin/sdkmanager --version

# Start the runner only after the Android SDK tools and environment are ready.
cd /home/$USER/actions-runner/
./svc.sh install $USER
./svc.sh start
