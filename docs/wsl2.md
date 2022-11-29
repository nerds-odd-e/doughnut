# WSL2 development environment setup

### 1. Ensure you install WSL2 with Ubuntu-20.04 or Ubuntu-22.04 from official Microsoft Store

Make sure you do not run WSL2 as `root` user for doughnut development enviroment. `nix` tool setup with `root` user will fail!

### 2. Additional packages to be installed for Cypress to work

```bash
sudo apt-get install libgtk2.0-0 libgtk-3-0 libgbm-dev libnotify-dev libgconf-2-4 libnss3 libxss1 libasound2 libxtst6 xauth xvfb
curl -sSL https://bina.egoist.sh/egoist/dum | sh
```

Follow the instruction from [this article](https://shouv.medium.com/how-to-run-cypress-on-wsl2-989b83795fb6) to setup an X-Server needed to run Cypress with GUI (i.e. `dum cy:open`)

Open your start menu > right-click XLaunch shortcut > More > Open file location;

Right-click XLaunch shortcut in the Explorer window > Properties;

Add `-ac` option right after the closing double quote in the Target field - i.e. "C:\Program Files\VcXsrv\xlaunch.exe" -ac;

If you still failed to open cypress after following above instructions, you may try replace `DISPLAY` by below commands in `.bashrc`.

```
export DISPLAY=$(route.exe print | grep 0.0.0.0 | head -1 | awk '{print $4}'):0.0
```

### 3. Git checkout as-is, commit Unix-style

```bash
git config --global core.autocrlf true
```

### 4. Git credentials

You can config wsl2 git to use windows credential manager to avoid personal access token by following [this article](https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-git)
