// Browser Mode setup - minimal mocks, real browser APIs!
// Note: vitest-dom/extend-expect doesn't work in Browser Mode (uses Node.js APIs)
// Browser Mode has built-in DOM matchers, so we don't need vitest-dom here

// Polyfill process.env for libraries that expect Node.js environment
if (typeof process === "undefined") {
  // @ts-expect-error - process is not defined in browser
  globalThis.process = {
    env: {},
  }
}

// Import CSS for proper rendering with Tailwind and DaisyUI
import "../src/assets/daisyui.css"
import "../src/index.css"

import { vi } from "vitest"
import createFetchMock from "vitest-fetch-mock"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()
fetchMock.doMock()

if (process.env.FRONTEND_UT_CONSOLE_OUTPUT_AS_FAILURE) {
  const CONSOLE_FAIL_TYPES = ["error", "warn", "log"]

  CONSOLE_FAIL_TYPES.forEach((type) => {
    const originalConsole = console[type]
    console[type] = (message) => {
      originalConsole(message)
      throw new Error(`Failing due to console.${type} while running test!`)
    }
  })
}
