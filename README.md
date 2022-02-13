# Doughnut

![dough CI CD](https://github.com/nerds-odd-e/doughnut/workflows/dough%20CI%20CD/badge.svg) [![Join the chat at https://gitter.im/Odd-e-doughnut/community](https://badges.gitter.im/Odd-e-doughnut/community.svg)](https://gitter.im/Odd-e-doughnut/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## About

Doughnut is a Personal Knowledge Management ([PKM](https://en.wikipedia.org/wiki/Personal_knowledge_management)) tool combining [zettelkasten](https://eugeneyan.com/writing/note-taking-zettelkasten/) style of knowledge capture with some features to enhance learning (spaced-repetition, smart reminders) and ability to share knowledge bits with other people (for buddy/team learning).

For more background info you can read:

- [Scholarship & Learning](https://www.lesswrong.com/tag/scholarship-and-learning)
- [Knowledge Acquisition & Documentation Structuring](https://en.m.wikipedia.org/wiki/Knowledge_Acquisition_and_Documentation_Structuring)

## [Product Backlog](https://docs.google.com/spreadsheets/d/1_GofvpnV1tjy2F_aaoOiYTZUOO-8t_qf3twIKMQyGV4/edit?ts=600e6711&pli=1#gid=0)

[Story Map](https://miro.com/app/board/o9J_lTB77Mc=/)

## How to Contribute

- We welcome product ideas and code contribution.
- Collaborate over:
  - [GitHub Discussions](https://github.com/nerds-odd-e/doughnut/discussions) for product ideas/features,
  - [GitHub Issues](https://github.com/nerds-odd-e/doughnut/issues) for reporting issues or bugs, OR
  - [doughnut gitter.im](https://gitter.im/Odd-e-doughnut/community)
- FOSS style; Fork and submit Github PR.
  - Please keep the PR small and on only one topic
  - The code need to come with tests

## Technology Stack

- [Nix](https://nixos.org/)
- [Zulu OpenJDK 17](https://docs.azul.com/core/zulu-openjdk/release-notes/january-2022)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Flyway](https://flywaydb.org)
- [Gradle](https://gradle.org/)
- [Junit5](https://junit.org/junit5/)
- [Cypress](https://www.cypress.io/)
- [Cucumber](https://cucumber.io/docs/guides/)
- [cypress-cucumber-preprocessor](https://github.com/TheBrainFamily/cypress-cucumber-preprocessor#cypress-configuration)
- [Vue3](https://v3.vuejs.org/guide/introduction.html)
- [Vite](https://vitejs.dev)
- [Jest](https://jestjs.io/)
- [Vue Testing Library](https://testing-library.com/docs/vue-testing-library/intro/)
- [Bootstrap 5](https://getbootstrap.com/docs/5.0/getting-started/introduction/)
- [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/)
- [Github Actions](https://docs.github.com/en/actions)
- [git-secret](https://git-secret.io)
- [SaltStack](https://docs.saltproject.io/en/latest/)
- [packer](https://www.packer.io)
- [packer googlecompute builder](https://www.packer.io/docs/builders/googlecompute)
- [Google Cloud](https://cloud.google.com/gcp/getting-started)
- [Google Cloud Managed Instance Group](https://cloud.google.com/compute/docs/instance-groups/)
- [Google Cloud SQL](https://cloud.google.com/sql/docs/mysql/introduction)

## Getting started

### 1. Quick Start - doughnut development environment setup

From the root of doughnut directory:

```bash
./setup-doughnut-dev.sh
```

To manually walk-thru the install process - see Step 2 below.

### 2. [Local development environment with nix](./docs/nix.md)

### 3. [Cloud IDE development environment with Gitpod.io](./docs/gitpod.md)

### 4. Setup and run doughnut with migrations in 'E2E' profile (backend app started on port 9081)

```bash
yarn sut
```

- Rerun it each time you reset the database.

#### Run full backend unit tests suite

- From doughnut source root directory:

```bash
yarn backend:test
```

### 5. End-to-End Test / Features / Cucumber / SbE / ATDD

We use cucumber [Gherkin](https://cucumber.io/docs/gherkin/) + cypress (test driver) Javascript/Typescript framework to drive the end to end test suite.

- [Cucumber](https://cucumber.io/)

The Cypress+Cucumber tests are in JavaScript/TypeScript.

[cypress](https://docs.cypress.io/guides/getting-started/writing-your-first-test#Add-a-test-file) + [cypress-cucumber-preprocessor](https://github.com/TheBrainFamily/cypress-cucumber-preprocessor)

#### Commands

| Purpose                               | Command (run from `doughnut` source root directory)                                                               |
| ------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| Install needed e2e tooling            | `yarn`                                                                                                            |
| Start SUT (backend system under test) | `yarn sut`                                                                                                        |
| Run all e2e test                      | `yarn test` (starts backend SUT and compile frontend and cypress headless)                                        |
| Run all e2e test with FE in dev mode  | `yarn test:dev` (starts backend SUT and frontend in dev mode and cypress headless)                                |
| Run cypress IDE                       | `yarn sut`, `yarn frontend:sut` and `yarn cy:open` (starts frontend SUT in dev mode, backend SUT and cypress IDE) |

#### Structure

| Purpose          | Location                                    |
| ---------------- | ------------------------------------------- |
| feature files    | `cypress/integration/*.feature`             |
| step definitions | `cypress/step_definitions/common/*.{js,ts}` |
| custom DSL       | `cypress/support/*.js`                      |
| cucumber hooks   | `cypress/step_definitions/common/hook.ts`   |
| test fixtures    | `cypress/fixtures/*.*`                      |
| cypress config   | `cypress/config/*.json`                     |
| cypress plugins  | `cypress/plugins/index.js`                  |

### 6. [Vue3 web-app frontend](https://flutter.dev/docs/get-started/web)

We chose Vue3 + Vite to build our frontend.

#### How-to

##### Run frontend unit tests

From `doughnut` source root directory

```bash
yarn frontend:test
```

##### Build & Bundle Vue3 frontend web-app assets and startup backend app (doughnut webapp will launch on port 9081).

```bash
yarn frontend:build
yarn sut
```

Expect to find minified and uglified web bundle assets in `backend/src/main/resources/static` directory:

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

### 7. [Production environment](./docs/prod_env.md)

### 8. [Doughnut source code secrets management](./docs/secrets_management.md)
