#!/bin/bash

sudo apt-get update && apt-get install jq curl unzip openjdk-17-jdk -y -qq

GITHUB_TOKEN=""
echo $GITHUB_TOKEN > /tmp/token.txt
RUNNER_VERSION="2.317.0"
SDK_VERSION="11076708"
RUNNER_ORG=""
RUNNER_TOKEN=$(curl -s -L -X POST -H "Accept: application/vnd.github+json" -H "Authorization: Bearer $GITHUB_TOKEN" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/$RUNNER_ORG/actions/runners/registration-token | jq -r .token)


# Install Runner
useradd -m -d /home/runner -s /bin/bash runner
sudo usermod -aG sudo runner
echo "runner ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
whoami > /tmp/id.txt
mkdir -p actions-runner
cd actions-runner
curl -o actions-runner-linux-x64-$RUNNER_VERSION.tar.gz -L https://github.com/actions/runner/releases/download/v$RUNNER_VERSION/actions-runner-linux-x64-$RUNNER_VERSION.tar.gz
tar xzf ./actions-runner-linux-x64-$RUNNER_VERSION.tar.gz

su - runner -c "
/home/runner/actions-runner/config.sh --url https://github.com/$RUNNER_ORG --token $RUNNER_TOKEN --unattended"

cd /home/runner/actions-runner/
./svc.sh install runner
./svc.sh start

# Install Android SDK
su - runner -c "
wget https://dl.google.com/android/repository/commandlinetools-linux-"$SDK_VERSION"_latest.zip
mkdir -p ~/android/cmdline-tools/latest
mkdir -p ~/android/tmp
unzip ~/commandlinetools-linux-"$SDK_VERSION"_latest.zip -d ~/android/tmp/
mv ~/android/tmp/cmdline-tools/* ~/android/cmdline-tools/latest/"
