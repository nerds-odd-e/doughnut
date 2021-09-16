# Gitpod Dev Environment in the Cloud

## Pre-requisite

- Google Chrome or Chrome/Chromium derivative browser.
- A [Github](https://www.github.com).
- A [Gitpod](https://www.gitpod.io/#gAet-started) signed up using your Github account above.
- [Chrome extension for Gitpod](https://chrome.google.com/webstore/detail/gitpod-always-ready-to-co/dodmmooeoklaejobgleioelladacbeki).
- [More details/info](https://www.gitpod.io/docs/browser-extension/) about starting up your doughnut dev env in Gitpod.

## Up & running your Gitpod `doughnut` development environment
### Basic Gitpod VSCode workspace
- Visit [doughnut Github repository](https://github.com/nerds-odd-e/doughnut) from your chrome/chromium-derivative broswer; Click on the `Gitpod` green button (near the top right corner of `doughnut`'s Github page) to launch your development workspace.
- From your Gitpod VSCode workspace, bottom left hand corner, locate the `Login to Github` icon/button and perform your login by entering your relevant Github credentials in the new browser tab spawned.
- Once your Gitpod workspace with VSCode has launched successfully in a new chrome/chromium-derivative browser from above step, open a VSCode 'terminal'.

### zsh & git config to setup development workspace
- On the Gitpod workspace VSCode, start a zsh terminal.
- From Gitpod zsh terminal, configure your _git_ [user] block in `/home/gitpod/.gitconfig` to ensure your git commits are labeled correctly and also to get Gitpod-Github access for code push permission in place (you should already have been authorised for Github doughnut repo write access).

### Preparation steps to run doughnut backend unit tests & cypress End-to-End tests
- From the root of the `doughnut` codebase (this should be on path `/workspace/doughnut`), run `yarn` to get End-to-End testing tooling setup.
- From root of `doughnut` run `./gradlew bootRunE2E` to setup and migrate your base virgin `doughnut` DB tables via `flyway` migrations. Once the migrations have completed (read the `springboot` startup logs from the VSCode terminal), use `Ctrl-C` to exit `springboot` backend server application process.
- From `doughnut/frontend` path, also run `yarn` followed by `yarn build` to prepare for frontend Vue3 development tool packages setup.
- From root of `doughnut` source path, execute `yarn test:dev` to execute the full headless cypress End-to-End test suite.