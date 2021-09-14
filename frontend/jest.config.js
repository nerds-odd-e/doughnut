module.exports = {
  verbose: true,
  collectCoverage: false,
  globals: {
    "vue-jest": { babelConfig: true },
  },
  moduleFileExtensions: ["js", "ts", "json", "vue"],
  transform: {
    ".*\\.(vue)$": "vue-jest",
    ".*\\.(js)$": "babel-jest",
    "^.+\\.(ts|tsx)$": "ts-jest",
  },
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  setupFiles: ["./tests/setupJest.js"],
};
