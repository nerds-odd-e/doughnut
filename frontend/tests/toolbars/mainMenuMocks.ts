import type { MemoryTrackerLite } from "@generated/donut-backend-api"
import { vi } from "vitest"

export const memoryTrackerLitesStub = (n: number): MemoryTrackerLite[] =>
  Array.from({ length: n }, (_, i) => ({
    memoryTrackerId: i + 1,
    spelling: false,
  }))

export const defaultMenuData = {
  assimilationCount: {
    dueCount: 0,
    assimilatedCountOfTheDay: 0,
    totalUnassimilatedCount: 0,
  },
  recallStatus: {
    toRepeat: [] as MemoryTrackerLite[],
    currentRecallWindowEndAt: "",
    totalAssimilatedCount: 0,
  },
  unreadMessages: [],
}

export const createMenuData = (
  overrides?: Partial<typeof defaultMenuData>
) => ({
  ...defaultMenuData,
  ...overrides,
})

export function aiReplyEventSourceMockExports() {
  return {
    default: class {
      onMessage = vi.fn(() => this)
      onError = vi.fn(() => this)
      start = vi.fn()
    },
  }
}
