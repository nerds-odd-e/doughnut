import { vi } from "vitest"

export const mockedPush = vi.fn()

export function noteUndoButtonRouterMockExports(
  actual: typeof import("vue-router")
) {
  return {
    ...actual,
    useRoute: () => ({ path: "/", fullPath: "/" }),
    useRouter: () => ({
      push: mockedPush,
    }),
  }
}
