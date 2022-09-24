# WSL2 development environment setup

### 1. Ensure you install WSL2 with Ubuntu-20.04 or Ubuntu-22.04 from official Microsoft Store
Make sure you do not run WSL2 as `root` user for doughnut development enviroment. `nix` tool setup with `root` user will fail!

### 2. Additional packages to be installed for Cypress to work
```bash
sudo apt-get install libgtk2.0-0 libgtk-3-0 libgbm-dev libnotify-dev libgconf-2-4 libnss3 libxss1 libasound2 libxtst6 xauth xvfb
```
Follow the instruction from [this article](https://shouv.medium.com/how-to-run-cypress-on-wsl2-989b83795fb6) to setup an X-Server needed to run Cypress with GUI (i.e. `yarn cy:open`)

Open your start menu > right-click XLaunch shortcut > More > Open file location;

Right-click XLaunch shortcut in the Explorer window > Properties;

Add `-ac` option right after the closing double quote in the Target field - i.e. "C:\Program Files\VcXsrv\xlaunch.exe" -ac;
