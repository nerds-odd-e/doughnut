import * as matchers from "vitest-dom/matchers";
import createFetchMock from "vitest-fetch-mock";
import { vi, expect } from "vitest";
expect.extend(matchers);

const fetchMock = createFetchMock(vi, {
  fallbackToNetwork: false
});

fetchMock.enableMocks();

if(process.env.FRONTEND_UT_CONSOLE_OUTPUT_AS_FAILURE) {
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
HTMLCanvasElement.prototype.getContext = function() {
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

// Mock IntersectionObserver for tests
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
  takeRecords() {
    return []
  }
}
