module.exports = {
  verbose: true,
  testEnvironment: 'node',
  collectCoverage: false,
  globals: {
    '@vue/vue3-jest': { babelConfig: true },
    'ts-jest': { babelConfig: true },
  },
  moduleFileExtensions: ['js', 'ts', 'vue', 'json'],
  transform: {
    '^.+\\.(j|t)s?$': '@swc/jest',
    '.*\\.(vue)$': '@vue/vue3-jest',
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  setupFilesAfterEnv: ['./tests/setupJest.js'],
};
