import { createViewportCurrentAnchorDebouncer } from "@/lib/book-reading/debounceViewportCurrentAnchorId"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("createViewportCurrentAnchorDebouncer", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it("commits once with latest value after rapid proposes", () => {
    const commits: (number | null)[] = []
    const d = createViewportCurrentAnchorDebouncer({
      delayMs: 120,
      commit: (id) => commits.push(id),
    })
    d.propose(1)
    d.propose(2)
    d.propose(1)
    expect(commits).toEqual([])
    vi.advanceTimersByTime(120)
    expect(commits).toEqual([1])
  })

  it("does not commit when cancel runs before delay", () => {
    const commits: (number | null)[] = []
    const d = createViewportCurrentAnchorDebouncer({
      delayMs: 120,
      commit: (id) => commits.push(id),
    })
    d.propose(5)
    d.cancel()
    vi.advanceTimersByTime(120)
    expect(commits).toEqual([])
  })

  it("skips redundant propose matching last committed", () => {
    const commits: (number | null)[] = []
    const d = createViewportCurrentAnchorDebouncer({
      delayMs: 120,
      commit: (id) => commits.push(id),
    })
    d.propose(7)
    vi.advanceTimersByTime(120)
    expect(commits).toEqual([7])
    d.propose(7)
    vi.advanceTimersByTime(120)
    expect(commits).toEqual([7])
  })

  it("commits null after debounce when candidate becomes null", () => {
    const commits: (number | null)[] = []
    const d = createViewportCurrentAnchorDebouncer({
      delayMs: 50,
      commit: (id) => commits.push(id),
    })
    d.propose(3)
    vi.advanceTimersByTime(50)
    d.propose(null)
    vi.advanceTimersByTime(50)
    expect(commits).toEqual([3, null])
  })

  it("commitNow applies immediately and clears a pending propose", () => {
    const commits: (number | null)[] = []
    const d = createViewportCurrentAnchorDebouncer({
      delayMs: 120,
      commit: (id) => commits.push(id),
    })
    d.propose(1)
    d.commitNow(9)
    expect(commits).toEqual([9])
    vi.advanceTimersByTime(120)
    expect(commits).toEqual([9])
  })

  it("resets delay on each propose", () => {
    const commits: (number | null)[] = []
    const d = createViewportCurrentAnchorDebouncer({
      delayMs: 100,
      commit: (id) => commits.push(id),
    })
    d.propose(10)
    vi.advanceTimersByTime(80)
    d.propose(11)
    vi.advanceTimersByTime(80)
    expect(commits).toEqual([])
    vi.advanceTimersByTime(25)
    expect(commits).toEqual([11])
  })
})
