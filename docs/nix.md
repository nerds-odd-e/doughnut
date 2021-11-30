# Local development machine development environment setup with nix

### 1. Install nix

We use nix to manage and ensure a reproducible development environment ([nixos.org](https://nixos.org)).

#### For macOS:

Full details on nix installation on macOS [here](https://nixos.org/manual/nix/stable/#sect-macos-installation)

```bash
 sh <(curl -k -L https://nixos.org/nix/install) --darwin-use-unencrypted-nix-store-volume
```

#### For Linux:

```bash
sh <(curl -k -L https://nixos.org/nix/install) --daemon
```

(NB: if the install script fails to add sourcing of `nix.sh` in `.bashrc` or `.profile`, you can do it manually `source /etc/profile.d/nix.sh`)

### 2. Setup and run doughnut for the first time (local development profile)

Launch a new terminal in your favourite shell (I highly recommend zsh).

```bash
mkdir -p ~/.config/nix
echo 'experimental-features = nix-command flakes' >> ~/.config/nix/nix.conf
nix-channel --update; nix-env -iA nixpkgs.nix && nix-env -u --always
```

Clone full all-in-one doughnut codebase from Github

```bash
git clone https://github.com/nerds-odd-e/doughnut
```

Boot up your doughnut development environment.
MySQL DB server is started and initialised on entering the `nix develop`.

```bash
cd doughnut
export NIXPKGS_ALLOW_UNFREE=1
nix develop
```

Run E2E profile springboot backend server with gradle (backend app started on port 9081)

```bash
# from doughnut source root dir
yarn && yarn frontend:build
./gradlew bootRunE2E
open http://localhost:9081
```

Run E2E profile with backend server & frontend in dev mode & Cypress IDE (frontend app on port 3000; backend app on port 9081)

```bash
# from doughnut source root dir
yarn && yarn frontend:sut
./gradlew bootRunE2E
yarn cy:open
```

Run Dev profile springboot backend server with gradle (backend app started on port 9082)

```bash
# from doughnut source root dir
yarn && yarn frontend:build
./gradlew bootRunDev
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

- Locate your `nix develop` installed JDK path location from the header printout on entering ` nix develop`` ($JAVA_HOME is printed out on entering  `nix develop`).
  - e.g. On macOS this could look like `/nix/store/cj3vbr57and7wywlvac6dkz62kzf0awh-zulu16.30.15-ca-jdk-16.0.1/zulu-16.jdk/Contents/Home`.
- **File -> Project Structure -> Platform Settings -> SDKs -> Add JDK...**
  - Enter the full path of above (e.g. `/nix/store/cj3vbr57and7wywlvac6dkz62kzf0awh-zulu16.30.15-ca-jdk-16.0.1/zulu-16.jdk/Contents/Home`).
    ![Sample `nix develop` JAVA_HOME](./images/01_doughnut_nix-shell_JAVA_HOME.png "Sample nix-shell JAVA_HOME")

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
