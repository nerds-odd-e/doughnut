import { ref, type Ref } from "vue"
import type { Clock } from "./useThinkingTimeTracker"

// No prior precedent in the codebase. Genuine hard thinking with no mouse/
// keyboard/touch/scroll input is common, so this is deliberately generous
// (upper end of a 45-60s range) to avoid flagging normal pauses. Unlike
// away/detour, idle does NOT pause the clock: it only flags a stretch of
// thinkingTimeMs that the reviewer should discount.
const IDLE_THRESHOLD_MS = 60000

// Absence of mouse/keyboard/touch/scroll input while the question is
// on-screen and the clock is running (not paused for away/detour/viewing a
// previous answer). Stays inside thinkingTimeMs the whole time — this only
// accumulates the portion of an inactivity stretch beyond IDLE_THRESHOLD_MS
// once that stretch first crosses the threshold, so short pauses under the
// threshold contribute nothing. New activity resets detection for the next
// stretch but never retroactively removes idleMs already counted.
export function createIdleDetector(clock: Clock, isRunning: Ref<boolean>) {
  const idleMs = ref(0)
  const lastActivityAt = ref<number | null>(null)
  const idleAccumulatingSince = ref<number | null>(null)

  const recordActivity = () => {
    if (!isRunning.value) return
    lastActivityAt.value = clock.now()
    idleAccumulatingSince.value = null
  }

  const checkIdle = () => {
    if (!isRunning.value || lastActivityAt.value === null) return

    const now = clock.now()
    const sinceActivity = now - lastActivityAt.value
    if (sinceActivity < IDLE_THRESHOLD_MS) {
      idleAccumulatingSince.value = null
      return
    }

    if (idleAccumulatingSince.value === null) {
      idleAccumulatingSince.value = lastActivityAt.value + IDLE_THRESHOLD_MS
    }
    idleMs.value += now - idleAccumulatingSince.value
    idleAccumulatingSince.value = now
  }

  const markActivityAt = (timestamp: number) => {
    lastActivityAt.value = timestamp
  }

  return { idleMs, recordActivity, checkIdle, markActivityAt }
}
