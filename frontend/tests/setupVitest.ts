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

import { beforeEach, vi } from "vitest"
import createFetchMock from "vitest-fetch-mock"
import { emptyPopupStack } from "./helpers/popupStackTestSupport"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()
fetchMock.doMock()

beforeEach(() => {
  emptyPopupStack()
  window.history.replaceState(window.history.state, "", "/")
})

// Fail tests on Vue warnings and console.log usage
// Allow console.warn and console.error from libraries to pass through
const originalConsoleLog = console.log
const originalConsoleWarn = console.warn

// Fail on any console.log usage
console.log = (...args: unknown[]) => {
  originalConsoleLog(...args)
  throw new Error(`Failing due to console.log while running test!`)
}

// Fail only on Vue warnings; suppress known library noise in test output
console.warn = (...args: unknown[]) => {
  const message = args.join(" ")
  if (message.includes("decodeEntities option is passed")) {
    return
  }

  originalConsoleWarn(...args)

  if (message.includes("[Vue warn]")) {
    throw new Error(`Failing due to Vue warning while running test!`)
  }
}
