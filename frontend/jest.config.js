module.exports = {
  verbose: true,
  testEnvironment: "node",
  collectCoverage: false,
  globals: {
    "@vue/vue3-jest": { babelConfig: true },
    "ts-jest": { babelConfig: true },
  },
  moduleFileExtensions: ["js", "ts", "json", "vue"],
  transform: {
    ".*\\.(vue)$": "@vue/vue3-jest",
    "^.+\\.(t|j)sx?$": "@swc/jest",
  },
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  setupFilesAfterEnv: ["./tests/setupJest.js"],
};
