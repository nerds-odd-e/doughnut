const SUPPRESS_IDLE_GAP_MS = 100

export function createIntervalScrollSuppression() {
  let active = false
  let startTime = 0
  let lastEventTime = 0
  let maxMs = 0

  return {
    activate(holdMs: number) {
      const now = Date.now()
      active = true
      startTime = now
      lastEventTime = now
      maxMs = holdMs
    },
    checkEvent(): boolean {
      if (!active) return false
      const now = Date.now()
      if (
        now - startTime >= maxMs ||
        now - lastEventTime >= SUPPRESS_IDLE_GAP_MS
      ) {
        active = false
        return false
      }
      lastEventTime = now
      return true
    },
    reset() {
      active = false
    },
  }
}
