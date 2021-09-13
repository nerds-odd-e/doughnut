# Gitpod Dev Environment in the Cloud

## Pre-requisite

- Google Chrome or Chrome/Chromium derivative browser.
- A [Github](https://www.github.com).
- A [Gitpod](https://www.gitpod.io/#gAet-started) signed up using your Github account above.
- [Chrome extension for Gitpod](https://chrome.google.com/webstore/detail/gitpod-always-ready-to-co/dodmmooeoklaejobgleioelladacbeki).
- Visit [doughnut Github repository](https://github.com/nerds-odd-e/doughnut) from your chrome/chromium-derivative broswer; Click on the 'Gitpod' green button to launch your development workspace.
- [More details/info](https://www.gitpod.io/docs/browser-extension/) about starting up your doughnut dev env in Gitpod.
- Once your Gitpod workspace with VSCode has launched successfully in the new chrome/chromium-derivative browser from above, open a VSCode 'terminal'.
- From your Gitpod VSCode workspace, bottom left hand corner, locate the `Login to Github` icon/button and perform your login by entering your relevant Github credentials in the new browser tab spawned.
- On the Gitpod workspace VSCode terminal, type 'zsh' and answer the interactive questions to setup your zsh environment.
- Run `nix-shell` from the zsh terminal in VSCode - this will take a while; Be patient.
- Your `nix-shell` environment will boot up a virgin MySQL 8.0.x DB server with requisite relevant development packages and tooling installed.
- From nix-shell, configure your _git_ [user] block in `/home/gitpod/.gitconfig` to ensure your git commits are labeled correctly and also to get Gitpod-Github access for code push in place.
- From the root of the `doughnut` codebase (this should be on path `/workspace/doughnut`), run `yarn` to get End-to-End testing tooling setup.
- From root of `doughnut` run `./gradlew bootRunE2E` to setup your `doughnut` DB tables from `flyway` migrations. Once the migrations have completed (read the `springboot` startup logs from the VSCode terminal), use `Ctrl-C` to exit `springboot` backend server application process.
- From `doughnut/frontend` path, also run `yarn` followed by `yarn build` to prepare for frontend Vue3 development tool packages setup.
