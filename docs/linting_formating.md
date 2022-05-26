## [Style Guide](./docs/styleguide.md)

## Code linting and formating (cypress, frontend & backend code)

Details of yarn task aliases described below is visible in `doughnut/package.json`
and `doughnut/frontend/package.json`.

### 1. Linting

From `doughnut` source root directory, to lint all code, run:

```bash
yarn lint:all
```

### 2. Formatting

##### 2.1. Backend Java code formatting

From `doughnut` source root directory, run:

```bash
yarn backend:format
```

##### 2.2. Frontend Vue3/Typescript/Javascript code formatting

From `doughnut` source root directory, run:

```bash
yarn frontend:format
```

##### 2.3. Cypress Typescript/Javascript code formatting

From `doughnut` source root directory, run:

```bash
yarn cy:format
```
