# Doughnut

![dough CI CD](https://github.com/nerds-odd-e/doughnut/actions/workflows/ci.yml/badge.svg) [![Join the chat at https://gitter.im/Odd-e-doughnut/community](https://badges.gitter.im/Odd-e-doughnut/community.svg)](https://gitter.im/Odd-e-doughnut/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## About

Doughnut is a Personal Knowledge
Management ([PKM](https://en.wikipedia.org/wiki/Personal_knowledge_management)) tool
combining [zettelkasten](https://eugeneyan.com/writing/note-taking-zettelkasten/) style of knowledge
capture with some features to enhance learning (spaced-repetition, smart reminders) and ability to
share knowledge bits with other people (for buddy/team learning).

For more background info you can read::

- [Scholarship & Learning](https://www.lesswrong.com/tag/scholarship-and-learning)
- [Knowledge Acquisition & Documentation Structuring](https://en.m.wikipedia.org/wiki/Knowledge_Acquisition_and_Documentation_Structuring)

## [Product Backlog](https://doughnut.odd-e.com/n24336)

[Story Map](https://miro.com/app/board/o9J_lTB77Mc=/)

## [Doughnut Technology Stack](./docs/tech_stack.md)

## [Current Architecture Videos](./docs/current_architecture_workshops.md)

## Getting started

### 1. Quick Start - doughnut development environment setup

:checkered_flag: From the root of doughnut directory, in a terminal, run:

Please ensure your git configuration is appropriate for your OS to respect the correct line endings:

```bash
git config --global core.autocrlf input
git add --renormalize .
```

Install Nix package manager if you haven't already with

```bash
./setup-doughnut-dev.sh
```

For developers on macOS 15 Sequoia, please run the below if you face issue installing or starting up `nix` ([see here for full details](https://github.com/NixOS/nix/issues/10892))

``` bash
curl --proto '=https' --tlsv1.2 -sSf -L https://github.com/NixOS/nix/raw/master/scripts/sequoia-nixbld-user-migration.sh | bash -
```

Subsequently, each time your change directory into your `doughnut` cloned folder, `nix` flakes will be auto loaded. Each time you change directory away from `doughnut` directory, the `nix` environment will be auto unloaded.

Ensure your OS (WSL2/Ubuntu/Fedora, etc) has `/bin/sh` point to `bash`.
If you are using Ubuntu where `/bin/sh` is symlinked to `dash`, please
run `sudo dpkg-reconfigure dash` and answer "No" to reconfigure to `bash` as default.

:window: 🚨 **WSL2 with WSLg**:
#### [Additional things to note for Microsoft Windows10/Windows11 developers using WSL2g with Ubuntu-24.04.](./docs/wsl2.md)

#### 🚨 **DO NOT CLONE doughnut source to a MS Windows directory (e.g. `/mnt/c/`)!!!** Instead, in your WSL2 session, `cd ~` then `git clone git@github.com:nerds-odd-e/doughnut.git`

:warning: Nix and [sdkman](https://sdkman.io/) don't play very well together. A simple way around is to move or rename `~/.sdkman` dir and comment out sdkman related config in `~/.<SHELL>rc`.

:vertical_traffic_light: :construction: 🚨 **ONLY** if you hit problems with the above quick-start setup, you should manually walk through
the [local development environment nix setup](./docs/nix.md).

### 2. Setup and run doughnut with migrations in 'E2E' profile (backend app started on port 9081)

From the root of your doughnut directory, start your doughnut nix development environment with

```bash
nix develop
```

Start the backend service/application

```bash
pnpm sut
```

- Rerun it each time you reset the database or change backend java code (java is still a compiled language).

#### 2.1 Run full backend unit tests suite from terminal/CLI

- From doughnut source root directory:

```bash
pnpm backend:verify
```

### 3. [IntelliJ IDEA settings](./docs/idea.md)

### 4. End-to-End Test / Features / Cucumber / SbE / ATDD

We use cucumber [Gherkin](https://cucumber.io/docs/gherkin/) + cypress (test driver)
Javascript/Typescript framework to drive the end-to-end test suite.

- [Cucumber](https://cucumber.io/)

The Cypress+Cucumber tests are in JavaScript/TypeScript.

[cypress](https://docs.cypress.io/guides/getting-started/writing-your-first-test#Add-a-test-file)

- [cypress-cucumber-preprocessor](https://github.com/TheBrainFamily/cypress-cucumber-preprocessor)

- We use [mountebank](http://www.mbtest.org/) to mock external backend services.
  - To run mountebank in debug mode, run `pnpm mb --loglevel debug`.
 
#### Commands

For MS Windows WSL2 users:

1. you need to ensure your WSL2 Linux has `xvfb` installed manually before you can run cypress. This
   is not managed by Nix!
2. `export NODE_OPTIONS="--max-old-space-size=4096"` before running any cypress related commands (
   e.g. `cy:open` or `cypress run`).

| Purpose                               | Command (run from `doughnut` source root directory)                                                                                             |
| ------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------  |
| Install needed e2e tooling            | `pnpm --frozen-lockfile recursive install`                                                                                                      |                               |                                       |                                                                                                                                                 |
| Start SUT (backend system under test) | `pnpm sut` (starts backend SUT ONLY)                                                                                                            |
| Start Mock for external backend       | `pnpm start:mb` (starts mocked external backend ONLY)                                                                                           |
| Start ONLY the Cypress IDE            | `pnpm cy:open` (starts Cypress IDE ONLY)                                                                                                        |
| Run one feature headlessly            | `pnpm cypress run --spec **/name.feature` (expect services are already running, run the matched feature files only in headless mode)                 |
| Run all e2e test                      | `pnpm verify` (compile frontend assets, start backend SUT, mountebank virtual service provider & cypress headless e2e testing)                  |
| Run cypress with Backend & Frontend   | `pnpm test:open` (starts frontend SUT in HMR mode, backend SUT, mountebank virtual service provider & cypress IDE)                              |
| Generate TypeScript Interfaces        | `pnpm generateTypeScript` (Generate TypeScript Interfaces from backend JSON classes. Should run manually every time backend service changes)    |

#### Structure

| Purpose          | Location                                   |
| ---------------- |--------------------------------------------|
| feature files    | `e2e_test/features/*.feature`              |
| step definitions | `e2e_test/step_definitions/*.ts`           |
| custom DSL       | `e2e_test/support/*.ts`                    |
| cucumber hooks   | `e2e_test/step_definitions/common/hook.ts` |
| test fixtures    | `e2e_test/fixtures/*.*`                    |
| cypress config   | `e2e_test/config/*.json`                   |
| cypress plugins  | `e2e_test/plugins/index.ts`                |

### 5. Database migrations

You can find the database migrations in `backend/src/main/resources/db/migration/`.
The migrations are run automatically when the backend app starts up.
It will also run the migrations for test when you run `pnpm backend:test`.
To trigger the test DB migration manually, run `backend/gradlew migrateTestDB`.
To connect to the local DB: `mysql -S $MYSQL_HOME/mysql.sock -u doughnut -p` (password=doughnut).

### 6. Vue3 web-app frontend

We chose Vue3 + Vite to build our frontend.

The TypeScript code calling the backend services is generated from the backend code. Run

```bash
pnpm generateTypeScript
```

To do the code generation. There are two steps in this command:

1. Generate openAPI docs from the backend service into `./open_api_docs.yaml`.
2. Generate TypeScript interfaces from the openAPI docs, into `frontend/src/generated`.

If the step 1 is not done, a unit test will fail. If the step 2 is not done, CI will fail (`./assert_generated_type_script_up_to_date.sh`).

#### How-to

##### Run frontend unit tests (with Vitest)

From `doughnut` source root directory

```bash
pnpm frontend:verify
```

##### Run frontend web-app (app will launch on port 5173)

```bash
pnpm frontend:sut
```

##### Build & Bundle Vue3 frontend web-app assets and startup backend app (backend webapp will launch on port 9081)

```bash
pnpm frontend:build
pnpm sut
```

Expect to find minified and uglified web bundle assets in `backend/src/main/resources/static`
directory:

```bash
❯ pwd
/home/lia/doughnut/backend/src/main/resources/static
❯ tree -L 3
.
├── assets
│   ├── main.32137c85.js
│   ├── main.b097c993.css
│   └── vendor.8f9eb49d.js
├── index.html
├── odd-e.ico
└── odd-e.png

1 directory, 6 files
```

### 7. [Manual testing locally](./docs/manual_testing_locally.md)

When you have the backend and frontend running, you can manually test the application.

Visit http://localhost:5173/ to see the frontend web-app. The backend API is available at http://localhost:9081, and you will see the built frontend web-app there as well, but it wil not auto-reload on changes. So stay with the 5173 port.

Local test accounts:

- User: 'old_learner', Password: 'password'
- User: 'another_old_learner', Password: 'password'
- User: 'admin', Password: 'password'

### 8. [Style Guide & Code linting/formating](./docs/linting_formating.md)

### 9. [Production environment](./docs/prod_env.md)

### 10. [Doughnut source code secrets management](./docs/secrets_management.md)

### 11. Architecture and Design documentation

### 12. Teardown and cleanup

- pnpm: To clean up packages installed by pnpm, you can run pnpm store prune to remove unused packages from the store. To remove all packages for a specific project, navigate to the project directory and run pnpm recursive uninstall to uninstall all dependencies in the project and its subdirectories.
- Nix: If you want to remove the Nix package manager and all packages installed through it, you can run sudo rm -rf /nix to delete the Nix store. To uninstall Nix completely, follow the [official Nix documentation for uninstallation](https://nix.dev/manual/nix/2.22/installation/uninstall) instructions.

[Miro board](https://miro.com/app/board/uXjVNNaWVeA=/?share_link_id=753160038592)

## How to Contribute

- We welcome product ideas and code contribution.
- Collaborate over:
  - [GitHub Discussions](https://github.com/nerds-odd-e/doughnut/discussions) for product
    ideas/features,
  - [GitHub Issues](https://github.com/nerds-odd-e/doughnut/issues) for reporting issues or bugs, OR
  - [doughnut gitter.im](https://gitter.im/Odd-e-doughnut/community)
- FOSS style; Fork and submit GitHub PR.
  - Please keep the PR small and on only one topic
  - The code need to come with tests.
