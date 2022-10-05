# Doughnut
![dough CI CD](https://github.com/nerds-odd-e/doughnut/workflows/dough%20CI%20CD/badge.svg) [![Join the chat at https://gitter.im/Odd-e-doughnut/community](https://badges.gitter.im/Odd-e-doughnut/community.svg)](https://gitter.im/Odd-e-doughnut/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## About

Doughnut is a Personal Knowledge
Management ([PKM](https://en.wikipedia.org/wiki/Personal_knowledge_management)) tool
combining [zettelkasten](https://eugeneyan.com/writing/note-taking-zettelkasten/) style of knowledge
capture with some features to enhance learning (spaced-repetition, smart reminders) and ability to
share knowledge bits with other people (for buddy/team learning).

For more background info you can read::

- [Scholarship & Learning](https://www.lesswrong.com/tag/scholarship-and-learning)
- [Knowledge Acquisition & Documentation Structuring](https://en.m.wikipedia.org/wiki/Knowledge_Acquisition_and_Documentation_Structuring)

## [Product Backlog](https://docs.google.com/spreadsheets/d/1_GofvpnV1tjy2F_aaoOiYTZUOO-8t_qf3twIKMQyGV4/edit?ts=600e6711&pli=1#gid=0)

[Story Map](https://miro.com/app/board/o9J_lTB77Mc=/)

## How to Contribute

- We welcome product ideas and code contribution.
- Collaborate over:
  - [GitHub Discussions](https://github.com/nerds-odd-e/doughnut/discussions) for product
    ideas/features,
  - [GitHub Issues](https://github.com/nerds-odd-e/doughnut/issues) for reporting issues or bugs, OR
  - [doughnut gitter.im](https://gitter.im/Odd-e-doughnut/community)
- FOSS style; Fork and submit GitHub PR.
  - Please keep the PR small and on only one topic
  - The code need to come with tests

## [Doughnut Technology Stack](./docs/tech_stack.md)

## Getting started

### 1. Quick Start - doughnut development environment setup

From the root of doughnut directory:

```bash
./setup-doughnut-dev.sh
```

Ensure your OS (WSL2/Ubuntu/Fedora, etc) has `/bin/sh` point to `bash`.
If you are using Ubuntu where `/bin/sh` is symlinked to `dash`, please
run `sudo dpkg-reconfigure dash` and answer "No" to reconfigure to `bash` as default.

**WSL2**: [Additional things to note for Windows10 or Windows11 developers using WSL2 with Ubuntu-20.04 or Ubuntu-22.04.](./docs/wsl2.md)

:warning: Nix and [sdkman](https://sdkman.io/) don't play very well together. A simple way around is to move or rename `~/.sdkman` dir and comment out sdkman related config in `~/.<SHELL>rc`.

:vertical_traffic_light: :construction: **ONLY** if you hit problems with the above quick-start setup, you should manually walk through
the [local development environment nix setup](./docs/nix.md).


### 2. Setup and run doughnut with migrations in 'E2E' profile (backend app started on port 9081)

From the root of your doughnut directory, start your doughnut nix development environment with
```bash
nix develop
```

```bash
yarn && yarn frontend:build && yarn sut
```

- Rerun it each time you reset the database.

#### Run full backend unit tests suite

- From doughnut source root directory:

```bash
yarn backend:test
```

#### 2.1 Database migrations

You can find the database migrations in `backend/src/main/resources/db.migration/`.
The migrations are run automatically when the backend app starts up.
It will also run the migrations for test when you run `yarn backend:test`.
To trigger the test DB migration manually, run `backend/gradlew testDBMigrate`.

### 3. End-to-End Test / Features / Cucumber / SbE / ATDD

We use cucumber [Gherkin](https://cucumber.io/docs/gherkin/) + cypress (test driver)
Javascript/Typescript framework to drive the end-to-end test suite.

- [Cucumber](https://cucumber.io/)

The Cypress+Cucumber tests are in JavaScript/TypeScript.

[cypress](https://docs.cypress.io/guides/getting-started/writing-your-first-test#Add-a-test-file)

+ [cypress-cucumber-preprocessor](https://github.com/TheBrainFamily/cypress-cucumber-preprocessor)

#### Commands

For MS Windows WSL2 users:

1. you need to ensure your WSL2 Linux has `xvfb` installed manually before you can run cypress. This
   is not managed by Nix!
2. `export NODE_OPTIONS="--max-old-space-size=4096"` before running any cypress related commands (
   e.g. `cy:open` or `cy:run`).

| Purpose                               | Command (run from `doughnut` source root directory)                                                                                             |
|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| Install needed e2e tooling            | `yarn`                                                                                                                                          |
| Start SUT (backend system under test) | `yarn sut` (starts backend SUT ONLY)                                                                                                            |
| Run all e2e test                      | `yarn test` (compile frontend assets, start backend SUT, mountebank virtual service provider & cypress headless e2e testing)                    |
| Run all e2e test with FE in dev mode  | `yarn test:dev` (starts backend SUT, frontend SUT in HMR mode, mountebank virtual service provider & cypress headless e2e testing)              |
| Run cypress IDE                       | `yarn test:open` (starts frontend SUT in HMR mode, backend SUT, mountebank virtual service provider & cypress IDE)                              |
| Generate TypeScript Interfaces        | `yarn generateTypeScript` (Generate TypeScript Interfaces from backend JSON classes. Should run manually every time backend JSON class changes) |

#### Structure

| Purpose          | Location                                  |
|------------------|-------------------------------------------|
| feature files    | `cypress/integration/*.feature`           |
| step definitions | `cypress/step_definitions/*.ts`           |
| custom DSL       | `cypress/support/*.ts`                    |
| cucumber hooks   | `cypress/step_definitions/common/hook.ts` |
| test fixtures    | `cypress/fixtures/*.*`                    |
| cypress config   | `cypress/config/*.json`                   |
| cypress plugins  | `cypress/plugins/index.ts`                |

### 4. Vue3 web-app frontend

We chose Vue3 + Vite to build our frontend.

#### How-to

##### Run frontend unit tests

From `doughnut` source root directory

```bash
yarn frontend:test
```

##### Run frontend web-app (app will launch on port 5173)

```bash
yarn frontend:sut
```

##### Build & Bundle Vue3 frontend web-app assets and startup backend app (backend webapp will launch on port 9081).

```bash
yarn frontend:build
yarn sut
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

### 5. [Style Guide & Code linting/formating](./docs/linting_formating.md)

### 6. [Production environment](./docs/prod_env.md)

### 7. [Doughnut source code secrets management](./docs/secrets_management.md)

