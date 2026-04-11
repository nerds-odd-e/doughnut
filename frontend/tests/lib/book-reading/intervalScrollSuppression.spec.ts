import { createIntervalScrollSuppression } from "@/lib/book-reading/intervalScrollSuppression"
import { describe, expect, it, vi } from "vitest"

describe("createIntervalScrollSuppression", () => {
  it("suppresses rapid events (< 100ms gap)", () => {
    vi.useFakeTimers()
    try {
      const suppression = createIntervalScrollSuppression()
      suppression.activate(500)

      vi.advanceTimersByTime(50)
      expect(suppression.checkEvent()).toBe(true)
      vi.advanceTimersByTime(50)
      expect(suppression.checkEvent()).toBe(true)
      vi.advanceTimersByTime(50)
      expect(suppression.checkEvent()).toBe(true)
    } finally {
      vi.useRealTimers()
    }
  })

  it("releases on first event after >= 100ms gap from last event", () => {
    vi.useFakeTimers()
    try {
      const suppression = createIntervalScrollSuppression()
      suppression.activate(500)

      vi.advanceTimersByTime(50)
      expect(suppression.checkEvent()).toBe(true)

      // 110ms after the last suppressed event
      vi.advanceTimersByTime(110)
      expect(suppression.checkEvent()).toBe(false)
    } finally {
      vi.useRealTimers()
    }
  })

  it("releases when cumulative time exceeds holdMs regardless of event gaps", () => {
    vi.useFakeTimers()
    try {
      const suppression = createIntervalScrollSuppression()
      suppression.activate(500)

      // Rapid events at 80ms intervals — each gap < 100ms, but sum crosses 500ms
      for (let i = 0; i < 6; i++) {
        vi.advanceTimersByTime(80)
        suppression.checkEvent()
      }
      // 6 × 80ms = 480ms elapsed; next event at 560ms crosses the 500ms cap
      vi.advanceTimersByTime(80)
      expect(suppression.checkEvent()).toBe(false)
    } finally {
      vi.useRealTimers()
    }
  })
})
