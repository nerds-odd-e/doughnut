const path = require('path')
module.exports = {
  root: true,
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 2024,
    sourceType: 'module',
    extraFileExtensions: ['.vue'],
  },
  env: {
    browser: true,
    es2024: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:vue/vue3-strongly-recommended',
    'plugin:import/recommended',
    'plugin:import/typescript',
    'plugin:prettier/recommended',
    'prettier',
  ],
  ignorePatterns: [
    'dist',
    'node_modules',
    '*.md',
    'index.html',
    'components.d.ts',
    'index.d.ts',
    'shims-vue.d.ts',
    'tsconfig.json',
    'tsconfig.node.json',
    'farm.config.ts',
    'vite.config.ts',
    'vitest.config.ts',
    'setupVitest.js',
  ],
  settings: {
    'import/resolver': {
      typescript: {
        project: './tsconfig.json',
      },
      alias: {
        map: [['@', path.resolve(__dirname, './src')]],
        extensions: ['.js', '.ts', '.d.ts', '.vue'],
      },
      node: {
        extensions: ['.js', '.jsx', '.ts', '.tsx'],
      },
    },
    'import/core-modules': ['lodash'],
  },
  plugins: [
    'vue',
    '@typescript-eslint',
    'import',
    'prettier',
  ],
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'warn',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'warn',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    'import/no-unresolved': 'error',
    'import/named': 'error',
    'import/default': 'error',
    'import/namespace': 'error',
    'import/no-self-import': 'error',
    'import/no-cycle': ['error', { maxDepth: 1, ignoreExternal: true }],
    'vue/multi-word-component-names': 'off',
    'vue/require-default-prop': 'off',
    'vue/no-template-shadow': 'off',
    'vue/require-default-prop': 'off',
    'vue/no-template-shadow': 'off',
    'prettier/prettier': ['error', {
      semi: false,
      trailingComma: 'es5',
      bracketSpacing: true,
      printWidth: 80,
      arrowParens: 'always',
    }],
  },
  overrides: [
    {
      files: ['*.vue'],
      rules: {
        'no-undef': 'off',
      },
    },
    {
      files: ['*.ts', '*.tsx'],
      rules: {
        'no-undef': 'off',
      },
    },
    {
      files: ['*.config.js', '*.config.ts'],
      rules: {
        'import/no-extraneous-dependencies': 'off',
      },
    },
    {
      files: ['**/__tests__/**/*.[jt]s?(x)', '**/?(*.)+(spec|test).[jt]s?(x)'],
      extends: ['plugin:testing-library/vue'],
      plugins: ['vitest'],
      rules: {
        // Vitest specific rules
        'vitest/expect-expect': 'off',
        'vitest/no-disabled-tests': 'warn',
        'vitest/no-focused-tests': 'error',
        'vitest/no-identical-title': 'error',
      },
    },
  ],
}
