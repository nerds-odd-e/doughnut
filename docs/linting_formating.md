## [Style Guide](./docs/styleguide.md)

## Code linting and formating (cypress, frontend & backend code)

Details of yarn task aliases described below is visible in `doughnut/package.json`
and `doughnut/frontend/package.json`.

### 1. Formatting

From `doughnut` source root directory, run:

##### 1.1. Format All code
```bash
yarn format:all
```

##### 1.2. Backend Java code formatting
```bash
yarn backend:format
```

##### 1.3. Frontend Vue3/Typescript code formatting
```bash
yarn frontend:format
```

##### 1.4. Cypress E2E Typescript code formatting
```bash
yarn cy:format
```


### 2. Linting

From `doughnut` source root directory, run:

##### 2.1. Lint All code
```bash
yarn lint:all
```

##### 2.2. Backend Java code linting
```bash
yarn backend:lint
```

##### 2.3. Frontend Vue3/Typescript code linting
```bash
yarn frontend:lint
```

##### 2.4. Cypress E2E Typescript code linting
```bash
yarn cy:lint
```
