## [Style Guide](./docs/styleguide.md)

## Code linting and formating (cypress, frontend & backend code)

Details of pnpm task aliases described below is visible in `doughnut/package.json`
and `doughnut/frontend/package.json`.

### 1. VSCode IDE extension for Vue3 Typescript

##### 1.1. Install [Vue - Official VSCode Typescript extension for Vue3](https://marketplace.visualstudio.com/items?itemName=Vue.volar)

[Vue - Official](https://marketplace.visualstudio.com/items?itemName=Vue.volar) replaces Vetur, the previous
official VSCode extension for Vue 2.

If you have Vetur currently installed, make sure to disable it in Vue 3 projects.

##### 1.2. Turn on Volar Takeover mode

To enable Takeover Mode, you need to disable VSCode's built-in TS language service in your project's
workspace only by following these steps:

- In your project workspace, bring up the command palette with Ctrl + Shift + P (macOS: Cmd + Shift + P).
- Type built and select "Extensions: Show Built-in Extensions".
- Type typescript in the extension search box (do not remove @builtin prefix).
- Click the little gear icon of "TypeScript and JavaScript Language Features", and select "Disable (
  Workspace)".
- Reload the workspace. Takeover mode will be enabled when you open a Vue or TS file.
  ![Volar Takeover mode](./images/disable_vscode_typescript_extension.png "Disable VSCode default Typescript extension")

### 2. Formatting

From `doughnut` source root directory, run:

##### 2.1. Format All code

```bash
pnpm format:all
```

##### 2.2. Backend Java code formatting

```bash
pnpm backend:format
```

##### 2.3. Frontend Vue3/Typescript code formatting

```bash
pnpm frontend:format
```

##### 2.4. Cypress E2E Typescript code formatting

```bash
pnpm cy:format
```

### 3. Linting

Linting is mostly for the CI server to check for any leaked warnings or errors.
Developers should use format instead of lint.
From `doughnut` source root directory, run:

##### 3.1. Lint All code

```bash
pnpm lint:all
```

##### 3.2. Backend Java code linting

```bash
pnpm backend:lint
```

##### 3.3. Frontend Vue3/Typescript code linting

```bash
pnpm frontend:lint
```

##### 3.4. Cypress E2E Typescript code linting

```bash
pnpm cy:lint
```
