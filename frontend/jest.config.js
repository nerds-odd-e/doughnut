const { defaults } = require('jest-config');
module.exports = {
  verbose: true,
  testEnvironment: 'node',
  collectCoverage: false,
  globals: {
    '@vue/vue3-jest': { babelConfig: true },
    'ts-jest': { babelConfig: true },
  },
  moduleFileExtensions: [...defaults.moduleFileExtensions, 'js', 'ts', 'vue', 'json'],
  transform: {
    '^.+\\.(j|t)s?$': '@swc/jest',
    '.*\\.(vue)$': '@vue/vue3-jest',
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  testEnvironmentOptions: {
    customExportConditions: ["node", "node-addons"],
  },
  setupFilesAfterEnv: ['./tests/setupJest.js'],
};
