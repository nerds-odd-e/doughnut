# Cursor

Cursor is recommended for overall development for this project except backend.

## Monorepo TypeScript project setup

We use "pnpm" for monorepo TypeScript project setup. Both the frontend and e2e tests are written in TypeScript.

## Opening the Project in Cursor

To work on this project, you should have Cursor installed. If you haven't installed it yet, you can download it from [here](https://www.trycursor.com).

Once you have Cursor installed, follow these steps to open the project:

1. Open Cursor with `cursor .` from cloned `doughnut` source code `frontend` or `e2e_test` sub-directory.

Since this is a monorepo project managed by PNPM, each sub-project (like `frontend` and `e2e_test`) has its own `package.json` file (`e2e_test` `package.json` file is at root of `doughnut`).
Cursor might not automatically detect the correct `package.json` for each sub-project.
To help Cursor understand the structure of the project, you can create a multi-root workspace:

1. Open the Command Palette with `Ctrl+Shift+P` (or `Cmd+Shift+P` on macOS).
2. Type "Add Folder to Workspace" and select the command.
3. Navigate to each sub-project directory (like `frontend` and `e2e_test`) and click "Add".
4. Save the workspace with a name and location of your choice.

Now, each sub-project in your workspace should have its own isolated environment, and Cursor should use the correct `package.json` file for each sub-project.

Remember to run `pnpm recursive install` in the terminal at the root level of `doughnut` source directory to ensure all dependencies are correctly installed.

## Backend Java (Red Hat Java extension)

The repo `.vscode/settings.json` does **not** set `java.jdt.ls.java.home`, so Cursor starts even when `.nix/jdk` is missing (that path is created only after you run `pnpm setupCursorDev` from Nix, and it is gitignored).

For backend / Gradle work:

1. **Simplest:** Open the **repository root** in Cursor and use a **JDK 25** on your machine, or launch Cursor from a shell where you already ran `nix develop` so `JAVA_HOME` / `PATH` include Java.
2. **Nix JDK without launching from nix develop:** Run `CURSOR_DEV=true nix develop -c pnpm setupCursorDev`, then copy the optional `java.jdt.ls.java.home` line the script prints into **Cursor → Settings → open User Settings (JSON)** (not workspace settings, if the workspace JSON editor misbehaves). Use the printed **absolute** path to `.nix/jdk` (the symlink), not the `/nix/store/...` target, so it stays valid after you re-run the script.

If the warning persists after pulling this change, search **User** settings JSON for `java.jdt.ls.java.home`, `java.import.gradle.java.home`, or `java.configuration.runtimes` and remove or fix any entry that still points at `.nix/jdk` when that folder does not exist.
