# [Style Guide](./docs/styleguide.md)

## Code linting and formating (cypress, frontend & backend code)

Details of pnpm task aliases described below is visible in `doughnut/package.json`
and `doughnut/frontend/package.json`.

### 1. Cursor/VSCode IDE extension for Vue3, Cypress, Typescript, Javascript code formating/linting

#### 1.1. Install [Biome](https://biomejs.dev/guides/editors/first-party-plugins/)

[Biome](https://biomejs.dev/) One toolchain for your web project
Format, lint, and more in a fraction of a second.

#### 2.1. Format All code

From the root of your local `doughnut` source directory, run:

```bash
pnpm format:all
```

#### 2.2. Backend Java code formatting

From the root of your local `doughnut` source directory, run:

```bash
pnpm backend:format
```

#### 2.3. Frontend Vue3/Typescript code formatting

From the root of your local `doughnut` source directory, run:

```bash
pnpm frontend:format
```

#### 2.4. Cypress E2E Typescript code formatting

From the root of your local `doughnut` source directory, run:

```bash
pnpm cy:format
```

#### 2.5. OpenAPI documentation formatting

From the root of your local `doughnut` source directory, run:

```bash
pnpm openapi:lint
```

### 3. Linting

Linting is mostly for the CI server to check for any leaked warnings or errors.
Developers should use format instead of lint.

#### 3.1. Lint All code

From the root of your local `doughnut` source directory, run:

```bash
pnpm lint:all
```

#### 3.2. Backend Java code linting

From the root of your local `doughnut` source directory, run:

```bash
pnpm backend:lint
```

#### 3.3. Frontend Vue3/Typescript code linting

From the root of your local `doughnut` source directory, run:

```bash
pnpm frontend:lint
```

#### 3.4. Cypress E2E Typescript code linting

From the root of your local `doughnut` source directory, run:

```bash
pnpm cy:lint
```

#### 3.5. OpenAPI documentation linting

From the root of your local `doughnut` source directory, run:

```bash
pnpm openapi:lint
```

**Note:** The OpenAPI linting uses [Redocly CLI](https://redocly.com/docs/cli/) to validate the `open_api_docs.yaml` file. Configuration is defined in `redocly.yaml`.
