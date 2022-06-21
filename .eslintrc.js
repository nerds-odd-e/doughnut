module.exports = {
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint', 'cypress', 'prettier'],
  settings: {
    'import/resolver': {
      node: {
        extensions: ['.js', '.jsx', '.ts', '.tsx'],
      },
    },
  },
  extends: [
    'plugin:@typescript-eslint/recommended', // Uses the recommended rules from the @typescript-eslint/eslint-plugin
    'plugin:cypress/recommended',
    'plugin:prettier/recommended',
    'prettier',
  ],
  parserOptions: {
    ecmaVersion: 12, // Allows for the parsing of modern ECMAScript features
    sourceType: 'module', // Allows for the use of imports
    project: './cypress/tsconfig.json',
  },
  rules: {
    'prettier/prettier': [
      'warn',
      {
        printWidth: 100,
        tabWidth: 2,
        singleQuote: false,
        trailingComma: 'all',
        bracketSpacing: true,
        semi: false,
        useTabs: false,
      },
    ],
    '@typescript-eslint/no-var-requires': 'off',
    '@typescript-eslint/indent': 'off',
    '@typescript-eslint/no-use-before-define': 'warn',
    '@typescript-eslint/camelcase': 'off',
    '@typescript-eslint/member-delimiter-style': [
      'error',
      {
        multiline: {
          delimiter: 'none',
          requireLast: true,
        },
        singleline: {
          delimiter: 'semi',
          requireLast: false,
        },
      },
    ],
    '@typescript-eslint/explicit-function-return-type': 'off',
    '@typescript-eslint/no-explicit-any': 'warn',
    '@typescript-eslint/no-inferrable-types': [
      'warn',
      {
        ignoreProperties: true,
        ignoreParameters: true,
      },
    ],
    '@typescript-eslint/naming-convention': [
      'error',
      {
        selector: 'interface',
        format: ['PascalCase'],
        custom: {
          regex: '^I[A-Z]',
          match: true,
        },
      },
    ],
  },
};
