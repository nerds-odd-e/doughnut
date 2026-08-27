import { ref, onUnmounted, nextTick } from "vue"
import { createIdleDetector } from "./thinkingIdleDetection"

export type Clock = {
  now: () => number
}

const realClock: Clock = {
  now: () => performance.now(),
}

export type ThinkingTimeTrackerOptions = {
  clock?: Clock
}

// A device suspend (e.g. phone lock) leaves no pause() call behind, so a
// wall-clock jump this large between two checks can only be a suspend, never
// real event-loop delay. Anything beyond this is excluded from thinking time.
const SUSPEND_GAP_THRESHOLD_MS = 5000

export function useThinkingTimeTracker(
  options: ThinkingTimeTrackerOptions = {}
) {
  const clock = options.clock ?? realClock
  const accumulatedMs = ref(0)
  const runningStart = ref<number | null>(null)
  const isRunning = ref(false)
  const isPaused = ref(false)
  const hasStopped = ref(false)

  // Ticks every 250ms while running: reconciles suspend gaps (see
  // reconcileGap) and syncs pause state with document.hidden.
  let watchdogIntervalId: ReturnType<typeof setInterval> | null = null

  const clearWatchdog = () => {
    if (watchdogIntervalId !== null) {
      clearInterval(watchdogIntervalId)
      watchdogIntervalId = null
    }
  }

  const pause = () => {
    if (!isRunning.value || runningStart.value === null) return

    const now = clock.now()
    accumulatedMs.value += now - runningStart.value
    runningStart.value = null
    isRunning.value = false
    isPaused.value = true
    clearWatchdog()
  }

  // Tracks one category of interruption (away, detour, ...) as its own
  // paused-duration total and interruption count. Each category's
  // pauseFor()/resumeFrom() pair wraps the shared pause()/resume() so the
  // interruption is timed independently without double-pausing the tracker.
  const createInterruptionAccumulator = () => {
    const ms = ref(0)
    const count = ref(0)
    const pauseStart = ref<number | null>(null)

    const pauseFor = () => {
      if (!isRunning.value || runningStart.value === null) return

      pause()
      pauseStart.value = clock.now()
      count.value += 1
    }

    const resumeFrom = () => {
      if (pauseStart.value !== null) {
        ms.value += clock.now() - pauseStart.value
        pauseStart.value = null
      }
      resume()
    }

    return { ms, count, pauseFor, resumeFrom }
  }

  // Away: accumulated only from the tracker's own internal tab-away
  // detection (visibilitychange/pagehide/blur and the watchdog's
  // hidden-document sync) — never from externally invoked pause()/resume()
  // calls made by other composables for unrelated reasons.
  const away = createInterruptionAccumulator()

  // Detour: accumulated only from the publicly exposed
  // pauseForDetour()/resumeFromDetour() pair, called by an external
  // composable (useQuestionThinkingTime) when the learner navigates away to
  // a note/notebook mid-question and returns via Resume — never from the
  // tracker's own internal away detection or from plain pause()/resume().
  const detour = createInterruptionAccumulator()

  // Idle detection: see thinkingIdleDetection.ts for what it flags and why.
  const { idleMs, recordActivity, checkIdle, markActivityAt } =
    createIdleDetector(clock, isRunning)

  const reconcileGap = () => {
    if (!isRunning.value || runningStart.value === null) return

    const now = clock.now()
    const elapsed = now - runningStart.value
    if (elapsed <= SUSPEND_GAP_THRESHOLD_MS) {
      accumulatedMs.value += elapsed
    } else {
      // Dropped suspend gap: rebase idle too, or it reads as sleep-long idle.
      markActivityAt(now)
    }
    runningStart.value = now
  }

  const resume = () => {
    if (hasStopped.value) return
    if (isRunning.value) {
      // Already "running" per our state, but a device suspend fires no
      // pause() — this wake-up call is the first chance to reconcile it.
      reconcileGap()
      return
    }
    if (document.hidden) return

    runningStart.value = clock.now()
    markActivityAt(runningStart.value)
    isRunning.value = true
    isPaused.value = false

    clearWatchdog()
    watchdogIntervalId = setInterval(() => {
      if (hasStopped.value) {
        clearWatchdog()
        return
      }
      if (isRunning.value) {
        reconcileGap()
        if (document.hidden) {
          away.pauseFor()
        } else {
          checkIdle()
        }
      }
    }, 250)
  }

  const start = async () => {
    if (hasStopped.value) return

    await nextTick()
    requestAnimationFrame(() => {
      if (!hasStopped.value) {
        resume()
      }
    })
  }

  const updateAccumulatedTime = (): number => {
    reconcileGap()
    return Math.round(accumulatedMs.value)
  }

  const stop = (): number => {
    if (hasStopped.value) {
      return accumulatedMs.value
    }

    hasStopped.value = true

    updateAccumulatedTime()
    checkIdle() // flush in-progress idle before isRunning flips false below
    runningStart.value = null
    isRunning.value = false

    return Math.round(accumulatedMs.value)
  }

  const handleVisibilityChange = () => {
    if (document.hidden) {
      away.pauseFor()
    } else {
      away.resumeFrom()
    }
  }

  const handlePageHide = () => {
    away.pauseFor()
  }

  const handlePageShow = () => {
    away.resumeFrom()
  }

  const handleBlur = () => {
    away.pauseFor()
  }

  const handleFocus = () => {
    if (document.hidden) return
    away.resumeFrom()
  }

  const setupEventListeners = () => {
    document.addEventListener("visibilitychange", handleVisibilityChange)
    window.addEventListener("pagehide", handlePageHide)
    window.addEventListener("pageshow", handlePageShow)
    window.addEventListener("blur", handleBlur)
    window.addEventListener("focus", handleFocus)
    window.addEventListener("mousemove", recordActivity)
    window.addEventListener("keydown", recordActivity)
    window.addEventListener("click", recordActivity)
    window.addEventListener("touchstart", recordActivity)
    window.addEventListener("scroll", recordActivity, true)
  }

  const removeEventListeners = () => {
    document.removeEventListener("visibilitychange", handleVisibilityChange)
    window.removeEventListener("pagehide", handlePageHide)
    window.removeEventListener("pageshow", handlePageShow)
    window.removeEventListener("blur", handleBlur)
    window.removeEventListener("focus", handleFocus)
    window.removeEventListener("mousemove", recordActivity)
    window.removeEventListener("keydown", recordActivity)
    window.removeEventListener("click", recordActivity)
    window.removeEventListener("touchstart", recordActivity)
    window.removeEventListener("scroll", recordActivity, true)
  }

  setupEventListeners()

  onUnmounted(() => {
    removeEventListeners()
    clearWatchdog()
    pause()
  })

  return {
    start,
    stop,
    pause,
    resume,
    isRunning,
    isPaused,
    awayMs: away.ms,
    awayCount: away.count,
    pauseForDetour: detour.pauseFor,
    resumeFromDetour: detour.resumeFrom,
    detourMs: detour.ms,
    detourCount: detour.count,
    idleMs,
  }
}
