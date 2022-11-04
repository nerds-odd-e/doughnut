import createFetchMock from "vitest-fetch-mock";
// import * as matchers from "vitest-dom/matchers";
import { vi } from "vitest";
// expect.extend(matchers);
// import "@testing-library/jest-dom/extend-expect";

const fetchMock = createFetchMock(vi);

fetchMock.enableMocks();
// fetchMock.dontMock();

// Throw errors when a `console.error` or `console.warn` happens
// by overriding the functions
// const CONSOLE_FAIL_TYPES = ["error", "warn"];

// CONSOLE_FAIL_TYPES.forEach((type) => {
//   console[type] = (message) => {
//     throw new Error(
//       `Failing due to console.${type} while running test!\n\n${message}`
//     );
//   };
// });
