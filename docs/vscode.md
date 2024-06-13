# VSCode

VSCode is recommended for overall development for this project except backend.

## Monorepo TypeScript project setup

We use "pnpm" for monorepo TypeScript project setup. Both the frontend and e2e tests are written in TypeScript.

## Opening the Project in VSCode

To work on this project, you should have Visual Studio Code installed. If you haven't installed it yet, you can download it from [here](https://code.visualstudio.com/download).

Once you have VSCode installed, follow these steps to open the project:

1. Open VSCode.
2. Click on `File` in the menu bar, then select `Open...`.
3. Navigate to the directory where you have cloned this project, select it, and click `Open`.

Since this is a monorepo project managed by PNPM, each sub-project (like `frontend` and `e2e`) has its own `package.json` file. VSCode might not automatically detect the correct `package.json` for each sub-project. To help VSCode understand the structure of the project, you can create a multi-root workspace:

1. Open the Command Palette with `Ctrl+Shift+P` (or `Cmd+Shift+P` on macOS).
2. Type "Add Folder to Workspace" and select the command.
3. Navigate to each sub-project directory (like `frontend` and `e2e`) and click "Add".
4. Save the workspace with a name and location of your choice.

Now, each sub-project in your workspace should have its own isolated environment, and VSCode should use the correct `package.json` file for each sub-project.

Remember to run `pnpm recursive install` in the terminal at the root level of your project to ensure all dependencies are correctly installed.
