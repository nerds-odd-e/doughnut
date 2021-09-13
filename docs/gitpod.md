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

### nix-shell to setup development workspace tooling
- On the Gitpod workspace VSCode terminal, type 'zsh' and answer the interactive questions to setup your zsh environment.
- Run `nix-shell --command "zsh"` from the zsh terminal in VSCode - this will take a while; Be patient.
- Your `nix-shell` environment will boot up a virgin MySQL 8.0.x DB server with requisite relevant development packages and tooling installed.
- From nix-shell, configure your _git_ [user] block in `/home/gitpod/.gitconfig` to ensure your git commits are labeled correctly and also to get Gitpod-Github access for code push in place.

### Preparation steps to run cypress End-to-End tests
- From the root of the `doughnut` codebase (this should be on path `/workspace/doughnut`), run `yarn` to get End-to-End testing tooling setup.
- From root of `doughnut` run `./gradlew bootRunE2E` to setup your `doughnut` DB tables from `flyway` migrations. Once the migrations have completed (read the `springboot` startup logs from the VSCode terminal), use `Ctrl-C` to exit `springboot` backend server application process.
- From `doughnut/frontend` path, also run `yarn` followed by `yarn build` to prepare for frontend Vue3 development tool packages setup.
- From root of `doughnut` source path, execute `yarn test:dev` to execute the full headless cypress End-to-End test suite.