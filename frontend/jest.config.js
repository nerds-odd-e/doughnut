module.exports = {
  verbose: true,
  collectCoverage: false,
  globals: {
    "vue-jest": { babelConfig: true },
  },
  moduleFileExtensions: ["js", "json", "vue"],
  transform: {
    ".*\\.(vue)$": "vue-jest",
    ".*\\.(js)$": "babel-jest",
  },
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  setupFiles: ["./tests/setupJest.js"],
};
