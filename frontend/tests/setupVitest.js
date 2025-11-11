import { expect, vi } from "vitest";
import "vitest-dom/extend-expect";
import createFetchMock from "vitest-fetch-mock";

const fetchMock = createFetchMock(vi, {
  fallbackToNetwork: false
});

fetchMock.enableMocks();

if (process.env.FRONTEND_UT_CONSOLE_OUTPUT_AS_FAILURE) {
  const CONSOLE_FAIL_TYPES = ["error", "warn", "log"];

  CONSOLE_FAIL_TYPES.forEach((type) => {
    const originalConsole = console[type];
    console[type] = (message) => {
      originalConsole(message);
      throw new Error(
        `Failing due to console.${type} while running test!`
      );
    };
  });
}

// Mock FormData to make it easier to test
global.FormData = function () {
  return {
    _this_will_be_a_FormData_in_production: true,
    append(x, y) {
      Object.assign(this, { [x]: y });
    },
    toString() {
      return JSON.stringify(this);
    },
  };
}

// Mock canvas context for tests
HTMLCanvasElement.prototype.getContext = function () {
  return {
    drawImage: vi.fn(),
    fillRect: vi.fn(),
    fillStyle: "",
    clearRect: vi.fn(),
    getImageData: () => ({
      data: new Array(100),
    }),
    putImageData: vi.fn(),
    setTransform: vi.fn(),
    save: vi.fn(),
    restore: vi.fn(),
    scale: vi.fn(),
    translate: vi.fn(),
  }
}

// Mock IntersectionObserver for tests - Vitest 4.0 compatible constructor
function MockIntersectionObserver(callback, options) {
  this.callback = callback
  this.options = options
  this.disconnect = vi.fn()
  this.observe = vi.fn()
  this.unobserve = vi.fn()
  this.takeRecords = vi.fn(() => [])
}

global.IntersectionObserver = MockIntersectionObserver
