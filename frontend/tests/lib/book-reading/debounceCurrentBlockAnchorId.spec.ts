import { createCurrentBlockAnchorDebouncer } from "@/lib/book-reading/debounceCurrentBlockAnchorId"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("createCurrentBlockAnchorDebouncer", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it("commits once with latest value after rapid proposes", () => {
    const commits: (number | null)[] = []
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 120,
      commit: (id) => {
        commits.push(id)
        return true
      },
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
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 120,
      commit: (id) => {
        commits.push(id)
        return true
      },
    })
    d.propose(5)
    d.cancel()
    vi.advanceTimersByTime(120)
    expect(commits).toEqual([])
  })

  it("skips redundant propose matching last committed", () => {
    const commits: (number | null)[] = []
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 120,
      commit: (id) => {
        commits.push(id)
        return true
      },
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
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 50,
      commit: (id) => {
        commits.push(id)
        return true
      },
    })
    d.propose(3)
    vi.advanceTimersByTime(50)
    d.propose(null)
    vi.advanceTimersByTime(50)
    expect(commits).toEqual([3, null])
  })

  it("commitNow applies immediately and clears a pending propose", () => {
    const commits: (number | null)[] = []
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 120,
      commit: (id) => {
        commits.push(id)
        return true
      },
    })
    d.propose(1)
    d.commitNow(9)
    expect(commits).toEqual([9])
    vi.advanceTimersByTime(120)
    expect(commits).toEqual([9])
  })

  it("resets delay on each propose", () => {
    const commits: (number | null)[] = []
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 100,
      commit: (id) => {
        commits.push(id)
        return true
      },
    })
    d.propose(10)
    vi.advanceTimersByTime(80)
    d.propose(11)
    vi.advanceTimersByTime(80)
    expect(commits).toEqual([])
    vi.advanceTimersByTime(25)
    expect(commits).toEqual([11])
  })

  it("allows the same transition to be re-detected when commit returns false", () => {
    let rejectNext = true
    const commits: (number | null)[] = []
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 50,
      commit: (id) => {
        commits.push(id)
        const accepted = !rejectNext
        return accepted
      },
    })

    // First propose: commit called, returns false → lastCommitted resets to null
    d.propose(5)
    vi.advanceTimersByTime(50)
    expect(commits).toEqual([5])

    // Same value proposed again; since lastCommitted was reset, commit is called again
    rejectNext = false
    d.propose(5)
    vi.advanceTimersByTime(50)
    expect(commits).toEqual([5, 5])
  })

  it("rejected commitNow allows the same id to be committed again", () => {
    let reject = true
    const commits: (number | null)[] = []
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 50,
      commit: (id) => {
        commits.push(id)
        const accepted = !reject
        return accepted
      },
    })

    d.commitNow(3)
    expect(commits).toEqual([3])

    // rejected → lastCommitted back to null; next commitNow(3) should trigger again
    reject = false
    d.commitNow(3)
    expect(commits).toEqual([3, 3])
  })

  it("accepted commit prevents re-detecting the same transition", () => {
    const commits: (number | null)[] = []
    const d = createCurrentBlockAnchorDebouncer({
      delayMs: 50,
      commit: (id) => {
        commits.push(id)
        return true
      },
    })

    d.propose(7)
    vi.advanceTimersByTime(50)
    expect(commits).toEqual([7])

    // Same value: no commit because lastCommitted === 7
    d.propose(7)
    vi.advanceTimersByTime(50)
    expect(commits).toEqual([7])
  })
})
