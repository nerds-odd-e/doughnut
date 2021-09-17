module.exports = {
  root: true,
  parser: "@typescript-eslint/parser",
  env: {
    browser: true,
    es2021: true,
    jest: true,
  },
  extends: [
    "eslint:recommended",
    "airbnb-base",
    "plugin:import/recommended",
    "plugin:vue/vue3-strongly-recommended",
    "plugin:testing-library/vue",
    "plugin:@typescript-eslint/recommended",
    "prettier",
  ],
  settings: {
    "import/resolver": {
      alias: {
        map: [["@", "./src"]],
      },
    },
  },
  parserOptions: {
    ecmaVersion: 12,
    sourceType: "module",
  },
  plugins: ["vue", "testing-library", "@typescript-eslint", "jest-dom"],
  overrides: [
    {
      files: ["**/tests/**/*.[jt]s?(x)", "**/?(*.)+(spec|test).[jt]s?(x)"],
      extends: ["plugin:testing-library/vue"],
    },
  ],
  rules: {
    "no-unused-vars": ["error", { varsIgnorePattern: ".*", args: "none" }],
    "no-param-reassign": ["error", { props: false }],
    "import/no-cycle": ["error", { maxDepth: 1, ignoreExternal: true }],
    "import/no-self-import": "error",
    "testing-library/await-async-query": "error",
    "testing-library/no-await-sync-query": "error",
    "testing-library/no-debug": "warn",
    "testing-library/no-dom-import": "off",
    "no-useless-return": "off",
  },
};
