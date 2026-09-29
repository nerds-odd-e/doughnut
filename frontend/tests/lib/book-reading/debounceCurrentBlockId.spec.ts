import { createCurrentBlockIdDebouncer } from "@/lib/book-reading/debounceCurrentBlockId"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("createCurrentBlockIdDebouncer", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it("commits the latest value once the delay passes after rapid proposes", () => {
    const d = createCurrentBlockIdDebouncer({ delayMs: 120 })
    d.propose(1)
    d.propose(2)
    d.propose(3)
    expect(d.currentBlockId.value).toBeNull()
    vi.advanceTimersByTime(120)
    expect(d.currentBlockId.value).toBe(3)
  })

  it("does not commit when cancel runs before delay", () => {
    const d = createCurrentBlockIdDebouncer({ delayMs: 120 })
    d.propose(5)
    d.cancel()
    vi.advanceTimersByTime(120)
    expect(d.currentBlockId.value).toBeNull()
  })

  it("commits null after debounce when candidate becomes null", () => {
    const d = createCurrentBlockIdDebouncer({ delayMs: 50 })
    d.propose(3)
    vi.advanceTimersByTime(50)
    d.propose(null)
    vi.advanceTimersByTime(50)
    expect(d.currentBlockId.value).toBeNull()
  })

  it("commitNow applies immediately and clears a pending propose", () => {
    const d = createCurrentBlockIdDebouncer({ delayMs: 120 })
    d.propose(1)
    d.commitNow(9)
    expect(d.currentBlockId.value).toBe(9)
    vi.advanceTimersByTime(120)
    expect(d.currentBlockId.value).toBe(9)
  })

  it("resets delay on each propose", () => {
    const d = createCurrentBlockIdDebouncer({ delayMs: 100 })
    d.propose(10)
    vi.advanceTimersByTime(80)
    d.propose(11)
    vi.advanceTimersByTime(80)
    expect(d.currentBlockId.value).toBeNull()
    vi.advanceTimersByTime(25)
    expect(d.currentBlockId.value).toBe(11)
  })
})
