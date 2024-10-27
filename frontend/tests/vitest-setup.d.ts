import { MockInstance } from "vitest"

declare global {
  var fetchMock: MockInstance & {
    mockIf: (url: string, fn: () => string | Response | undefined) => void
    mockResponseOnce: (body: string) => void
    resetMocks: () => void
    once: (url: string, response: object) => void
  }
}

export {}
