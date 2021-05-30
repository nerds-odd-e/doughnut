# Doughnut

![dough CI CD](https://github.com/nerds-odd-e/doughnut/workflows/dough%20CI%20CD/badge.svg) [![Join the chat at https://gitter.im/Odd-e-doughnut/community](https://badges.gitter.im/Odd-e-doughnut/community.svg)](https://gitter.im/Odd-e-doughnut/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## About

Doughnut is a Personal Knowledge Management ([PKM](https://en.wikipedia.org/wiki/Personal_knowledge_management)) tool combining [zettelkasten](https://eugeneyan.com/writing/note-taking-zettelkasten/) style of knowledge capture with some features to enhance learning (spaced-repetition, smart reminders) and ability to share knowledge bits with other people (for buddy/team learning).

For more background info you can read:

- [Scholarship & Learning](https://www.lesswrong.com/tag/scholarship-and-learning)
- [Knowledge Acquisition & Documentation Structuring](https://en.m.wikipedia.org/wiki/Knowledge_Acquisition_and_Documentation_Structuring)

### Technology Stack

- [OpenJDK 11](https://openjdk.java.net/projects/jdk/11/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Flyway](https://flywaydb.org)
- [Thymeleaf](https://www.thymeleaf.org/)
- [Gradle](https://gradle.org/)
- [Junit5](https://junit.org/junit5/)
- [JavaScript](https://www.javascript.com)
- [Cypress](https://www.cypress.io/)
- [Cucumber](https://cucumber.io/docs/guides/)
- [cypress-cucumber-preprocessor](https://github.com/TheBrainFamily/cypress-cucumber-preprocessor#cypress-configuration)
- [Vue3](https://v3.vuejs.org/guide/introduction.html)
- [Vite](https://vitejs.dev)
- [Jest](https://jestjs.io/)
- [Vue Testing Library](https://testing-library.com/docs/vue-testing-library/intro/)
- [TailwindCSS](https://tailwindcss.com)
- [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/)
- [Google Cloud](https://cloud.google.com/gcp/getting-started)
- [Github Actions](https://docs.github.com/en/actions)
- [Nix](https://nixos.org/)
- [git-secret](https://git-secret.io)
- [packer](https://www.packer.io)
- [packer googlecompute builder](https://www.packer.io/docs/builders/googlecompute)
- [SaltStack](https://docs.saltproject.io/en/latest/)

## Getting started

### 1. Install nix

Find instruction at nixos.org (multi-user installation).

#### For macOS:

```
 sh <(curl -L https://nixos.org/nix/install) --darwin-use-unencrypted-nix-store-volume
```

#### For Linux:

```
sh <(curl -L https://nixos.org/nix/install) --daemon
```

(NB: if the install script fails to add sourcing of `nix.sh` in `.bashrc` or `.profile`, you can do it manually `source /etc/profile.d/nix.sh`)

_Install `any-nix-shell` for using `fish` or `zsh` in nix-shell_

```
nix-env -i any-nix-shell -f https://github.com/NixOS/nixpkgs/archive/master.tar.gz
```

##### `zsh`

Add the following to your _~/.zshrc_.
Create it if it doesn't exist.

```
any-nix-shell zsh --info-right | source /dev/stdin
```

### 2. Setup and run doughnut for the first time (local development profile)

The default spring profile is 'test' unless you explicitly set it to 'dev'. Tip: Add `--Dspring.profiles.active=${profile}"` to gradle task command.
MySQL DB server is started and initialised on entering the `nix-shell`.

Update/refresh your installed nix state and version

```bash
nix-channel --update; nix-env -iA nixpkgs.nix && nix-env -u --always
```

Clone and launch local development environment (backend app started on port 9082)

```bash
git clone $this_repo
cd doughnut
export NIXPKGS_ALLOW_UNFREE=1
nix-shell --pure --command "zsh"
gradle wrapper --distribution-type all
./gradlew bootRunDev"
open http://localhost:9082
```

#### IntelliJ IDEA (Community) IDE project import

```bash
nohup idea-community &
# open doughnut project in idea
# click import gradle project
# wait for deps resolution
# restore gradle wrapper if missing
```

#### Setup IntelliJ IDEA with JDK11 SDK

- Locate your `nix` installed JDK11 path location with `which java`.
  e.g. `/nix/store/5ib97va5ngfacdqzzcvxff62rjwkxajg-zulu11.2.3-jdk11.0.1/bin/java`.
- **File -> Project Structure -> Platform Settings -> SDKs -> Add JDK...**
  - Enter the full path of above (e.g. `/nix/store/5ib97va5ngfacdqzzcvxff62rjwkxajg-zulu11.2.3-jdk11.0.1`).

#### Run a single targetted JUnit5 test in IntelliJ IDEA

- Setup IntelliJ in Gradle perspective -> Gradle Settings (Wrench Icon) -> Run tests with -> IntelliJ IDEA
- Locate your test file in IDE (e.g. `backend/src/test/com/odde/doughnut/controllers/NoteRestControllerTests.java`).
  - Locate specific test method to run and look out for green run arrow icon in line number gutter.
  - Click on the green run arrow icon to kick off incremental build and single test run.

#### MySQL DB UI Client - DBeaver

```
nohup dbeaver &
```

### 3. Setup and run doughnut with migrations in 'test' profile (backend app started on port 8081)

```bash
./gradlew bootRunTest
```

#### Run full backend unit tests suite

```bash
./gradlew test
```

### 4. End-to-End Test / Features / Cucumber / SbE / ATDD

We use cucumber + cypress + Javascript library to do end to end test.

- [Cucumber](https://cucumber.io/)

#### Commands

| Purpose                       | Command                               |
| ----------------------------- | ------------------------------------- |
| run all e2e test              | `yarn test`                           |
| run cypress IDE               | `yarn cy:open`                        |
| start SUT (system under test) | `yarn sut` (Not needed for yarn test) |

#### Structure

| Purpose          | Location                            |
| ---------------- | ----------------------------------- |
| feature files    | `/cypress/integration/**`           |
| step definitions | `/cypress/support/step_definitions` |

#### How-to

The Cypress+Cucumber tests are written in JavaScript.

[cypress](https://docs.cypress.io/guides/getting-started/writing-your-first-test#Add-a-test-file) + [cypress-cucumber-preprocessor](https://github.com/TheBrainFamily/cypress-cucumber-preprocessor)

##### Run doughnut E2E tests in 'e2e' profile with Cypress IDE activated (backend app started on port 9081)

```bash
yarn open
```

##### Run doughnut E2E tests in 'e2e' profile headless (backend app started on port 9081)

```bash
yarn test
```

### 5. [Vue3 web-app frontend](https://flutter.dev/docs/get-started/web)

We chose Vue3 + Vite to build our light frontend.

#### How-to

From `frontend` directory

```bash
cd frontend
yarn
```

##### Run frontend unit tests

```bash
cd frontend
yarn test
```

##### Build & Bundle Vue3 frontend web-app assets and startup backend app (backend app started on port 8081)

```bash
cd frontend
yarn build
cd ..
./gradlew bootRun
```

Expect to find minified and uglified web bundle assets in `backend/src/main/resources/static` directory:

```bash
❯ pwd
/home/csd/csd/doughnut/backend/src/main/resources/static
❯ tree
.
├── assets
│   ├── index.2cfdfd84.js
│   ├── index.805e3910.css
│   └── vendor.7a53bc23.js
├── blog
│   ├── blog_landing.html
│   └── blog_landing.js
├── favicon.ico
├── img
│   └── odd-e.png
├── index.html
└── odd-e.ico

3 directories, 9 files
```

### 6. Interacting with gcloud CLI for cloud infrastructure management

- [Install `Google Cloud SDK`](https://cloud.google.com/sdk/docs/install)
- [Create App Server in GCloud Compute](infra/scripts/create-gcloud-app-compute.sh)
- Login to gcloud sdk: `gcloud auth login`
- Check your login: `gcloud auth list`
- Set/Point to gcloud dough project: `gcloud config set project carbon-syntax-298809`
- Check you can see the project as login user: `gcloud config list`

### 7. View/tail GCP VM instance logs

```bash
gcloud auth login
gcloud config set project carbon-syntax-298809
# Query GCP MIG instance/s health state and grep instance id of each GCP VM in MIG
infra/scripts/check-mig-doughnut-app-service-health.sh
# Expected output
# ❯ ./check-mig-doughnut-app-service-health.sh
# ---
# backend: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instanceGroups/doughnut-app-group
# status:
#  healthStatus:
#  - healthState: HEALTHY
#    instance: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instances/doughnut-app-group-0c2b
#    ipAddress: 10.142.0.7
#    port: 8081
#  - healthState: HEALTHY
#    instance: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instances/doughnut-app-group-2j9f
#    ipAddress: 10.142.0.8
#    port: 8081
#  kind: compute#backendServiceGroupHealth

# View instance logs - Take/use one of the above healthcheck report instance id for next command (e.g. doughnut-app-group-2j9f)
infra/scripts/view-mig-doughnut-app-instance-logs.sh doughnut-app-group-2j9f

# Tail instance logs - Take/use one of the above healthcheck report instance id for next command (e.g. doughnut-app-group-2j9f)
infra/scripts/tail-mig-doughnut-app-instance-logs.sh doughnut-app-group-2j9f
```

### 8. Building/refreshing doughnut-app MIG VM instance/s base image with Packer + GoogleCompute builder

We use packer + googlecompute builder + shell provisioner to construct and materialise base VM image to speed up deployment and control our OS patches and dependent packages and libraries upgrades

- [Packer](https://www.packer.io)
- [packer googlecompute builder](https://www.packer.io/docs/builders/googlecompute)
- [SaltStack](https://docs.saltproject.io/en/latest/)

#### How-to

From `infra` directory, run the following:

Login to dough GCP project account with `gcloud auth login`
Configure gcloud CLI to project ID with `gcloud config set project carbon-syntax-298809`

```bash
export GCLOUDSDK_CORE_PROJECT="$(gcloud config get-value project)"
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/carbon-syntax-298809-f31377ba77a9.json
PACKER_LOG=1 packer build packer.json
```

Expect to see following log line towards end of Packer build stdout log:
`--> googlecompute: A disk image was created: doughnut-debian10-mysql80-base-saltstack`

### 9. [Product Backlog](https://docs.google.com/spreadsheets/d/1_GofvpnV1tjy2F_aaoOiYTZUOO-8t_qf3twIKMQyGV4/edit?ts=600e6711&pli=1#gid=0)

[Story Map](https://miro.com/app/board/o9J_lTB77Mc=/)

### 10. How to Contribute

- We welcome product ideas and code contribution.
- Collaborate over:
  - [GitHub Discussions](https://github.com/nerds-odd-e/doughnut/discussions) for product ideas/features,
  - [GitHub Issues](https://github.com/nerds-odd-e/doughnut/issues) for reporting issues or bugs, OR
  - [doughnut gitter.im](https://gitter.im/Odd-e-doughnut/community)
- FOSS style; Fork and submit Github PR.
  - Please keep the PR small and on only one topic
  - The code need to come with tests

April SG CSD Circle Invitation code: rPSG5ZOD5lnK4Oc

### 11. Secrets via [git-secret](https://git-secret.io) and [GnuPG](https://www.devdungeon.com/content/gpg-tutorial)

#### Generate your local GnuPG key

- Generate your GnuPG key 4096 bits key using your odd-e.com email address with no-expiry (option 0 in dialog):

```
gpg --full-generate-key
```

- Export your GnuPG public key:

```
gpg --export --armor <your_email>@odd-e.com > <your_email>_public_gpg_key.gpg
```

- Email your GnuPG public key file <your_email>\_public_gpg_key.gpg from above step and private message an existing git-secret collaborator

#### Add a new user's GnuPG public key to local dev machine key-ring for git-secret for team secrets collaboration

- Add public key to local GnuPG key-ring: `gpg --import <your_email>_public_gpg_key.gpg`
- Add user to git-secret managed list of users: `git secret tell <your_email>@odd-e.com`
- Re-encrypt all managed secret files: `git secret hide -d`

#### List who are list of users managed by git-secret and allowed to encrypt/decrypt those files

- Short list of user emails of managed users: `git secret whoknows`
- List of user emails with expiration info of managed users: `git secret whoknows -l`

#### Removes a user from list of git-secret managed users (e.g. user should no longer be allowed access to list of secrets)

```
git secret killperson <user_to_be_removed_email>@odd-e.com
```

#### Add a new file for git-secret to manage

- Remove sensitive file from git: `git rm --cached <the_secret_file>`
- Tell git-secret to manage the file (auto add to .gitignore and update stuff in .gitsecret dir): `git secret add <the_secret_file>`
- Encrypt the file (need to reveal and hide for changes in list of users in dough/secrets*public_keys dir*): `git secret hide`

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
