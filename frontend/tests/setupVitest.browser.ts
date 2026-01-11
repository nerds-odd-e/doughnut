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

// Note: No fetch mocking, no FormData mocking, no IntersectionObserver mocking
// Browser Mode provides real implementations!
//
// If you need to mock network requests, consider using:
// - MSW (Mock Service Worker) - recommended for Browser Mode
// - Playwright's page.route() for request interception
//
// For module mocks (vi.mock), you can still use them in Browser Mode
// but browser APIs like IntersectionObserver, FormData, Canvas, etc.
// are now real and don't need mocking.
