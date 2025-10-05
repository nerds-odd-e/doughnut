# Option 2: Use a cloud virtual machine

1. Download and install Microsoft Remote Desktop or any other app that allows you to do remote login via [RDP](https://en.wikipedia.org/wiki/Remote_Desktop_Protocol)  
2. In Microsoft Remote Desktop, Add PC (get IP address from us); remove all Display options, set default resolution to 1600x1000, select 16-bit colors.
3. Log in with user/password (lia/lia)
4. First time login you will be greeted with "Welcome to Ubuntu 24.04!". Click on "Start setup", then "Next" twice (keep default options).
5. Open a terminal window (press ⌘ (CMD) or ⊞ (Win) to see installed applications) or launch IntelliJ or VScode and open embedded terminal from there. (see fig. below)
6. Run `cd ~/doughnut`
7. Run `nix develop` to setup the required tooling and boot up biome server and MySQL 8.4 DB server locally
8. Run `pnpm backend:sut` to bootup backend Java Springboot server with auto-reload and get all needed DB migrations to run (`Ctrl-C` twice to exit)
9. Run `pnpm verify`. This will run all end-to-end tests in headless mode
10. If you wish to continue to use git over HTTPS authentication, proceed to setup your GitHub Personal Access Token (since the local doughnut repository is initially cloned via HTTPS)
- Go to your GitHub settings
- Click on "Developer settings"
- Select "Personal access tokens"
- Click on "Generate new token (classic)"
- Give your token a descriptive name
- Select the "repo" scope, and click "Generate token."
- **Important:** Make sure to copy your new personal access token. You won't be able to see it again!
- run `git config --global credential.helper cache`
- Each time you're prompted for a username and password, use your GitHub username and the personal access token, respectively.
11. You may refer [here](./git+https_to_git+ssh_doughnut_repo_auth.md) for instructions on how to convert your locally cloned git HTTPS remote-origin to a git+ssh remote-origin which will then use ssh authentication instead
