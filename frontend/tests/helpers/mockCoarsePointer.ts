import { vi } from "vitest"

export function mockCoarsePointer(coarse: boolean) {
  return vi.spyOn(window, "matchMedia").mockImplementation((query) => ({
    matches: coarse && query === "(pointer: coarse)",
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }))
}
