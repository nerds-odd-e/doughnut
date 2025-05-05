# WSL2 development environment setup

## :warning: 🚨 **DO NOT CLONE doughnut source to a MS Windows directory (e.g. `/mnt/c/`)!!!** Instead, in your WSL2 session, `cd ~` then `git clone git@github.com:nerds-odd-e/doughnut.git`

### 1. Ensure you install WSL2g with Ubuntu-24.04 from official Microsoft Store

Follow this [youtube video tutorial on how to install WSL2 with WSLg](https://www.youtube.com/watch?v=FQ6ahcJOVz0) (Linux GUI enabled in WSL2) so you can run cypress in `cypress:open` mode.
Make sure you do not run WSL2g as `root` user for doughnut development enviroment. `nix` tool setup with `root` user will fail!

### 2. Additional packages to be installed for Cypress to work

```bash
sudo apt-get update -y && sudo apt-get upgrade -y && sudo apt-get install -y libgtk2.0-0 libgtk-3-0 libgbm-dev libnotify-dev libgconf-2-4 libnss3 libxss1 libasound2 libxtst6 xauth xvfb
```

For **Ubuntu 24.04** use the following command:

```bash
sudo apt-get update -y && sudo apt-get upgrade -y && sudo apt-get install -y libgtk2.0-0t64 libgtk-3-0t64 libgbm-dev libnotify-dev libnss3 libxss1 libasound2t64 libxtst6 libatomic1 libatk-bridge2.0-0 libcups2 libdrm2 libgbm1 xauth xvfb
```

Follow the instruction from [this article](https://shouv.medium.com/how-to-run-cypress-on-wsl2-989b83795fb6) to setup an X-Server needed to run Cypress with GUI (i.e. `pnpm cy:open`)

Open your start menu > right-click XLaunch shortcut > More > Open file location;

Right-click XLaunch shortcut in the Explorer window > Properties;

Add `-ac` option right after the closing double quote in the Target field - i.e. "C:\Program Files\VcXsrv\xlaunch.exe" -ac;

If you still failed to open cypress after following above instructions, you may try replace `DISPLAY` by below commands in `.bashrc`.

```bash
export DISPLAY=$(route.exe print | grep 0.0.0.0 | head -1 | awk '{print $4}'):0.0
```

### 3. Git checkout as-is, commit Unix-style

```bash
git config --global core.autocrlf input
git add --renormalize .
```

### 4. Git credentials

You can config wsl2 git to use windows credential manager to avoid personal access token by following [this article](https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-git)
