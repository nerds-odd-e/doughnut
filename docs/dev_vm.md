## Option 2: Use a cloud virtual machine

1. Download and install Microsoft Remote Desktop or any other app that allows you to do remote login via RDP (https://en.wikipedia.org/wiki/Remote_Desktop_Protocol)  
2. In Microsoft Remote Desktop, Add PC (get IP address from us); remove all Display options, set default resolution to 1600x1000, select 16-bit colors.
3. Log in with user/password (lia/lia)
4. First time login you will be greeted with "Welcome to Ubuntu 22.04!". Click on "Start setup", then "Next" twice (keep default options).
5. Open a terminal window (press ⌘ (CMD) or ⊞ (Win) to see installed applications) or launch IntelliJ or VScode and open embedded terminal from there. (see fig. below)
6. Run `cd ~/doughnut && nix develop`
7. Run `pnpm test`. This will run all end-to-end tests in headless mode. 
8. Setup GitHub Personal Access Token (since the doughnut repo is cloned via HTTPS).
  * Go to your GitHub settings
  * Click on "Developer settings"
  * Select "Personal access tokens"
  * Click on "Generate new token (classic)"
  * Give your token a descriptive name
  * Select the "repo" scope, and click "Generate token."
  * **Important:** Make sure to copy your new personal access token. You won’t be able to see it again!
  * run `git config --global credential.helper cache`
  * Each time you're prompted for a username and password, use your GitHub username and the personal access token, respectively.

