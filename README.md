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

### 4. Secrets via [git-secret](https://git-secret.io) and [GnuPG](https://www.devdungeon.com/content/gpg-tutorial)
#### Generate your local GnuPG key
- Generate your GnuPG key 4096 bits key using your odd-e.com email address with no-expiry (option 0 in dialog): `gpg --full-generate-key`
- Export your GnuPG public key: `gpg --export <your_email>@odd-e.com --armor > <your_email>_public_gpg_key.gpg`
- Copy and paste your GnuPG public key file from above step into dough/secrets_public_keys dir
#### Add a new user's GnuPG public key to local dev machine key-ring for git-secret for team secrets collaboration
- Add public key to local GnuPG key-ring: `gpg --import public_keys/<your_email>_public_gpg_key.gpg`
- Add user to git-secret managed list of users: `git secret tell <your_email>@odd-e.com`
- Re-encrypt all managed secret files: `git secret hide`
#### List who are list of users managed by git-secret and allowed to encrypt/decrypt those files
- Short list of user emails of managed users: `git secret whoknows`
- List of user emails with expiration info of managed users: `git secret whoknows -l`
#### Removes a user from list of git-secret managed users (e.g. user should no longer be allowed access to list of secrets)
- `git secret killperson <user_to_be_removed_email>@odd-e.com`
#### Add a new file for git-secret to manage
- Remove sensitive file from git: `git rm --cached <the_secret_file>`
- Tell git-secret to manage the file (auto add to .gitignore and update stuff in .gitsecret dir): `git secret add <the_secret_file>`
- Encrypt the file (need to reveal and hide for changes in list of users in dough/secrets_public_keys dir_): `git secret hide`
#### View diff of git-secret managed files
- `git secret changes -p <your__gpg_passphrase>`
#### List all git-secret managed files
- `git secret list`
#### Remove a git-secret file from git-secret management (make sure you reveal/decrypt it before doing this!!!)
- Just remove file from git-secret management but leaves it on the filesystem: `git secret remove <your__no_longer_secret_file>`
- Remove an encrypted file from git-secret management and permanently delete it from filesystem (make sure you have revealed/decrypted the file): `git secret remove -c <your_no_longer_secret_file>`
#### Reveal all git-secret managed encrypted files
- Upon hitting `enter/return` for each decrypt command below, enter secret passphrase you used when you generated your GnuPG key-pair.
- Decrypt secrets to local filesystem: `git secret reveal`
- Decrypt secrets to stdout: `git secret cat`

### 5. Create gcloud compute instance
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

### 6. Check gcloud compute instance startup logs
```
gcloud compute instances get-serial-port-output doughnut-instance --zone us-east1-b
```

### 7. End-to-End Test / Features / Cucumber / SbE / ATDD

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
