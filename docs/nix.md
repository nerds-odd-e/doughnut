# Local development machine development environment setup with nix

### 1. Install nix

We use nix to manage and ensure a reproducible development environment ([nixos.org](https://nixos.org)).

#### For macOS:

Full details on nix installation on macOS [here](https://nixos.org/manual/nix/stable/#sect-macos-installation)

```bash
 sh <(curl -L https://nixos.org/nix/install) --darwin-use-unencrypted-nix-store-volume
```

#### For Linux:

```bash
sh <(curl -L https://nixos.org/nix/install) --daemon
```

(NB: if the install script fails to add sourcing of `nix.sh` in `.bashrc` or `.profile`, you can do it manually `source /etc/profile.d/nix.sh`)

_Install `any-nix-shell` for using `fish` or `zsh` in nix-shell_

```bash
# OPTIONAL
nix-env -i any-nix-shell -f https://github.com/NixOS/nixpkgs/archive/master.tar.gz
```

##### `zsh`

Add the following to your _~/.zshrc_.
Create it if it doesn't exist.

```bash
# OPTIONAL
any-nix-shell zsh --info-right | source /dev/stdin
```

### 2. Setup and run doughnut for the first time (local development profile)

The default spring profile is 'test' unless you explicitly set it to 'dev'. Tip: Add `--Dspring.profiles.active=${profile}"` to gradle task command.
MySQL DB server is started and initialised on entering the `nix-shell`.

Update/refresh your installed nix state and version

```bash
# OPTIONAL
nix-channel --update; nix-env -iA nixpkgs.nix && nix-env -u --always
```

Clone full all-in-one doughnut codebase from Github

```bash
git clone https://github.com/nerds-odd-e/doughnut
```

Launch local nix-shell development environment (with zsh)

```bash
cd doughnut
export NIXPKGS_ALLOW_UNFREE=1
nix-shell --pure --command "zsh"
```

Bootup springboot backend server with gradle (backend app started on port 9082)

```bash
gradle wrapper --distribution-type all
./gradlew bootRunDev"
open http://localhost:9082
```

#### IntelliJ IDEA (Community) IDE project import

```bash
nohup idea-community &
# open doughnut project in idea
# click import gradle project
# wait for deps resolution
# restore gradle wrapper if missing
```

#### Setup IntelliJ IDEA with JDK16 SDK

- Locate your `nix` installed JDK16 path location with `which java` (it is printed out on entering `nix-shell`).
  - e.g. `/nix/store/60kc2wrpr8p0jb8hginzq2hmhi9l9ws0-zulu16.30.15-ca-jdk-16.0.1`.
- **File -> Project Structure -> Platform Settings -> SDKs -> Add JDK...**
  - Enter the full path of above (e.g. `/nix/store/60kc2wrpr8p0jb8hginzq2hmhi9l9ws0-zulu16.30.15-ca-jdk-16.0.1`).

#### Run a single targetted JUnit5 test in IntelliJ IDEA

- Setup IntelliJ in Gradle perspective -> Gradle Settings (Wrench Icon) -> Run tests with -> IntelliJ IDEA
- Locate your test file in IDE (e.g. `backend/src/test/com/odde/doughnut/controllers/NoteRestControllerTests.java`).
  - Locate specific test method to run and look out for green run arrow icon in line number gutter.
  - Click on the green run arrow icon to kick off incremental build and single test run.

#### VSCodium IDE for frontend JS/TS development

```bash
nohup codium &
```

#### MySQL DB UI Client - DBeaver

```bash
nohup dbeaver &
```
