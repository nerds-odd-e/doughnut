import fetchMock from 'jest-fetch-mock';
import '@testing-library/jest-dom/extend-expect';

fetchMock.enableMocks();

// Throw errors when a `console.error` or `console.warn` happens
// by overriding the functions
const dev_mode=process.env.DEVELOPMENT_MODE
if(dev_mode !== "true") {
  const CONSOLE_FAIL_TYPES = ['error', 'warn']

  CONSOLE_FAIL_TYPES.forEach((type) => {
    console[type] = (message) => {
      throw new Error(
        `Failing due to console.${type} while running test!\n\n${message}`,
      )
    }
  })
}
