# Ona Dev Environment in the Cloud

## Pre-requisite

- Google Chrome or Chrome/Chromium derivative browser. Ensure the browser is configured to allow new tab spawning/popup for URL pattern `[*.ona.io]`.
- A [Github](https://www.github.com) account (authroised for [doughnut Github](https://github.com/nerds-odd-e/doughnut) repo write access).
- A [Ona](https://app.gitpod.io) account signed up using your Github account above.
- _Optional_ [Chrome extension for Ona](https://chromewebstore.google.com/detail/ona/dodmmooeoklaejobgleioelladacbeki). This chrome extension gives you a nice green 'Ona' button at the top of [doughnut Github](https://github.com/nerds-odd-e/doughnut) repo to launch your Ona workspace. You may also enter the URL [https://app.gitpod.io/#https://github.com/nerds-odd-e/doughnut/](https://app.gitpod.io/#https://github.com/nerds-odd-e/doughnut/) to achieve the same effect.

## Up & running your Ona `doughnut` development environment

### Basic Ona VSCode workspace

- Visit [doughnut Github](https://github.com/nerds-odd-e/doughnut) repo from your chrome/chromium-derivative broswer; Click on the `Ona` green button (near the top right corner of `doughnut`'s Github page if you have installed the chrome extension from above prerequisite step) to launch your development workspace. Or enter in your chomre browser's URL input [https://app.gitpod.io/#https://github.com/nerds-odd-e/doughnut/](https://app.gitpod.io/#https://github.com/nerds-odd-e/doughnut/).
- From your Ona VSCode workspace browser tab launched from above step, at the bottom left hand corner of the VSCode IDE, locate the `Login to Github` icon/button and perform your Github login by entering your relevant Github credentials in the new browser tab spawned.
- Once your Ona workspace with VSCode has launched successfully in a new chrome/chromium-derivative browser from above step, open a VSCode 'terminal'. (after VSCode browser tab is launched, another tab will launch which is a VNC connection to your ona for Cypress IDE launch for local development E2E testing use).

### zsh & git config to setup development workspace

- On the Ona workspace VSCode, start a zsh terminal.
- From Ona zsh terminal, configure your _git_ `user.name` and `user.email` to ensure your git commits are labeled correctly and also to get Ona-Github access for code push permission in place (you should already have been authorised for Github doughnut repo write access).

```bash
git config user.name "Your beautiful name"
git config user.email "your_email@your_domain.com"
```

### Get your doughnut DB tables setup

- From root of `doughnut` run `pnpm sut` to setup and migrate your base virgin `doughnut` DB tables via `flyway` migrations. Once the migrations have completed (read the `springboot` startup logs from the VSCode terminal), use `Ctrl-C` to exit `springboot` backend server application process. (this might take some time - once done, `Ctrl-C` to exit process on completion).

### Preparation steps to run doughnut backend unit tests & cypress End-to-End tests

- From the root of the `doughnut` codebase (this should be on path `/workspace/doughnut`), boot up your `nix` development environment (with `direnv` just `cd` to the `doughtnut` source directory, else, `nix develop` to prepare End-to-End testing tooling setup.
- From root of `doughnut` source path, execute `pnpm verify` to execute the full headless cypress End-to-End test suite.

### Running java springboot unit tests

- From the root of the `doughnut` codebase, run `pnpm backend:verify`. This assumes you have had your doughnut DB tables setup from above.

### Running frontend Vue3 unit tests

- Navigate to `doughnut` source root directory. Execute `pnpm frontend:verify` to run the full frontend unit tests ONCE.
- Navigate to `doughnut` source root directory. Execute `pnpm frontend:test:watch` to run the full frontend unit tests in dev-mode with HMR live reload as you make frontend production/test code changes.
