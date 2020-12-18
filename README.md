# Doughnut

## About

Doughnut is a Personal Knowledge Management ([PKM](https://en.wikipedia.org/wiki/Personal_knowledge_management)) tool combining [zettelkasten](https://eugeneyan.com/writing/note-taking-zettelkasten/) style of knowledge capture with some features to enhance learning (spaced-repetition, smart reminders) and ability to share knowledge bits with other people (for buddy/team learning).

## Getting started

### 1. Install nix

Find instruction at nixos.org (multi-user installation).

For macOS:
```
 sh <(curl -L https://nixos.org/nix/install) --darwin-use-unencrypted-nix-store-volume
```

For Linux:
```
sh <(curl -L https://nixos.org/nix/install) --daemon
```

(NB: if the install script fails to add sourcing of `nix.sh` in `.bashrc` or `.profile`, you can do it manually `source /etc/profile.d/nix.sh`)


### 2. Setup and run doughnut for the first time
The default spring profile is 'test' unless you explicitly set it to 'dev'. Tip: Add `--args="--spring.profiles.active={profile}"` to gradle task command. 

```bash
git clone $this_repo
cd doughnut
nix-shell --pure
idea-community &
# open doughnut project in idea
# click import gradle project
#wait for deps resolution
cd backend
./gradlew bootRun --args="--spring.profiles.active=dev"
# open localhost:8080
```

### 3. Setup and run doughnut with migrations in 'test' profile
TODO: MAKE MIGRATIONS RUN BEFORE TEST
```bash
./gradlew bootRun --args='--spring.profiles.active=test'
```

### 4. Create gcloud compute instance
```
gcloud compute instances create doughnut-instance \
  --image-family debian-10 \
  --image-project debian-cloud \
  --machine-type g1-small \
  --scopes "userinfo-email,cloud-platform" \
  --metadata-from-file startup-script=instance-startup.sh \
  --metadata BUCKET=dough-01 \
  --zone us-east1-b \
  --tags http-server
```

### 5. Check gcloud compute instance startup logs
```
gcloud compute instances get-serial-port-output doughnut-instance --zone us-east1-b
```

### 6. End-to-End Test / Features / Cucumber / SbE / ATDD

We use cucumber + cypress + Java library to do end to end test.

#### Commands

| purpose | command |
|--------| --------|
| run all e2e test | `yarn test`   |
| run cypress IDE  | `yarn cy:open`|
| start SUT (system under test)| `yarn sut` (Not needed for yarn test)|

#### Structure

| purpose | location |
|--------| --------|
| feature files | `/cypress/integration/**`   |
| step definitions  | `/cypress/support/step_definitions`|

#### How to

The Cypress+Cucumber tests are written in JavaScript.
