module.exports = {
  parser: "@typescript-eslint/parser",
  plugins: [
    "@typescript-eslint",
    "cypress",
    "chai-friendly",
    "prettier",
    "unused-imports",
    "testing-library"
  ],
  settings: {
    "import/resolver": {
      node: {
        extensions: [".cjs", ".js", ".ts"],
      },
    },
  },
  extends: [
    "plugin:@typescript-eslint/recommended", // Uses the recommended rules from the @typescript-eslint/eslint-plugin
    "plugin:cypress/recommended",
    "plugin:prettier/recommended",
    "prettier",
  ],
  parserOptions: {
    ecmaVersion: 15, // Allows for the parsing of modern ECMAScript features
    sourceType: "module", // Allows for the use of imports
    project: "tsconfig.json",
    tsconfigRootDir: __dirname
  },
  ignorePatterns: [
    "tsconfig.json",
    ".eslintrc.cjs",
    "config/ci.ts",
    "config/common.ts"
  ],
  rules: {
    "no-unused-expressions": "off",
    "chai-friendly/no-unused-expressions": "error",
    "cypress/no-assigning-return-values": "error",
    "cypress/no-unnecessary-waiting": "error",
    "cypress/no-async-tests": "error",
    "cypress/no-pause": "error",
    "cypress/assertion-before-screenshot": "warn",
    "cypress/unsafe-to-chain-command": "off",
    "prettier/prettier": [
      "warn",
      {
        printWidth: 100,
        tabWidth: 2,
        singleQuote: false,
        trailingComma: "all",
        bracketSpacing: true,
        semi: false,
        useTabs: false,
      },
    ],
    "@typescript-eslint/no-var-requires": "off",
    "@typescript-eslint/indent": "off",
    "@typescript-eslint/no-use-before-define": "warn",
    "@typescript-eslint/camelcase": "off",
    "@typescript-eslint/member-delimiter-style": [
      "error",
      {
        multiline: {
          delimiter: "none",
          requireLast: true,
        },
        singleline: {
          delimiter: "semi",
          requireLast: false,
        },
      },
    ],
    "@typescript-eslint/explicit-function-return-type": "off",
    "@typescript-eslint/no-explicit-any": "warn",
    "@typescript-eslint/no-inferrable-types": [
      "warn",
      {
        ignoreProperties: true,
        ignoreParameters: true,
      },
    ],
    "@typescript-eslint/no-unused-vars": "off",
    "unused-imports/no-unused-imports": "error",
    "unused-imports/no-unused-vars": [
      "warn",
      { "vars": "all", "varsIgnorePattern": "^_", "args": "after-used", "argsIgnorePattern": "^_" }
    ]
  },
};
