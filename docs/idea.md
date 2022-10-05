# IntelliJ IDEA (Community) IDE project setup

Launch your IntelliJ IDE from your host OS.

### Setup IntelliJ IDEA with JDK17 SDK

- Locate your `nix develop` installed JDK path location from the header printout on entering ` nix develop` ($JAVA_HOME is printed to stdout on entering `nix develop`).
  - e.g. On macOS this could look like `/nix/store/yrai8hf3qkkz1a7597y1hkhwi52zamcs-zulu17.30.15-ca-jdk-17.0.1/zulu-17.jdk/Contents/Home`.
- **File -> Project Structure -> Platform Settings -> SDKs -> Add JDK...**
  - Enter the full path of above (e.g. `/nix/store/yrai8hf3qkkz1a7597y1hkhwi52zamcs-zulu17.30.15-ca-jdk-17.0.1/zulu-17.jdk/Contents/Home`).
    ![Sample `nix develop` JAVA_HOME](./images/01_doughnut_nix_develop_JAVA_HOME.png "Sample nix develop JAVA_HOME")
  - Ensure JDK level is set to LTS version 17
    ![Project Structure -> Project SDK -> Language Level](./images/jdk17_language_level.png "Language Level Setting")

### Run a single targetted JUnit5 test in IntelliJ IDEA

- Setup IntelliJ in Gradle perspective -> Gradle Settings (Wrench Icon) -> Run tests with -> IntelliJ IDEA
  - ![Gradle Settings](./images/gradle_settings.png "Gradle Settings")
  - ![Gradle Run tests with IDEA](./images/gradle_jvm_run_tests_with_idea.png "Run tests with IDEA")
- Locate your test file in IDE (e.g. `backend/src/test/com/odde/doughnut/controllers/NoteRestControllerTests.java`).
  - Locate specific test method to run and look out for green run arrow icon in line number gutter.
  - Click on the green run arrow icon to kick off incremental build and single test run.
