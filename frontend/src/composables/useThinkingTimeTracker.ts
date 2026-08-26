import { ref, onUnmounted, nextTick } from "vue"

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

  // Away time/count: accumulated only from the tracker's own internal
  // tab-away detection (visibilitychange/pagehide/blur and the watchdog's
  // hidden-document sync) — never from externally invoked pause()/resume()
  // calls made by other composables for unrelated reasons.
  const awayMs = ref(0)
  const awayCount = ref(0)
  const awayPauseStart = ref<number | null>(null)

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

  const pauseForAway = () => {
    if (!isRunning.value || runningStart.value === null) return

    pause()
    awayPauseStart.value = clock.now()
    awayCount.value += 1
  }

  const resumeFromAway = () => {
    if (awayPauseStart.value !== null) {
      awayMs.value += clock.now() - awayPauseStart.value
      awayPauseStart.value = null
    }
    resume()
  }

  const reconcileGap = () => {
    if (!isRunning.value || runningStart.value === null) return

    const now = clock.now()
    const elapsed = now - runningStart.value
    if (elapsed <= SUSPEND_GAP_THRESHOLD_MS) {
      accumulatedMs.value += elapsed
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
          pauseForAway()
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
    runningStart.value = null
    isRunning.value = false

    return Math.round(accumulatedMs.value)
  }

  const handleVisibilityChange = () => {
    if (document.hidden) {
      pauseForAway()
    } else {
      resumeFromAway()
    }
  }

  const handlePageHide = () => {
    pauseForAway()
  }

  const handlePageShow = () => {
    resumeFromAway()
  }

  const handleBlur = () => {
    pauseForAway()
  }

  const handleFocus = () => {
    if (document.hidden) return
    resumeFromAway()
  }

  const setupEventListeners = () => {
    document.addEventListener("visibilitychange", handleVisibilityChange)
    window.addEventListener("pagehide", handlePageHide)
    window.addEventListener("pageshow", handlePageShow)
    window.addEventListener("blur", handleBlur)
    window.addEventListener("focus", handleFocus)
  }

  const removeEventListeners = () => {
    document.removeEventListener("visibilitychange", handleVisibilityChange)
    window.removeEventListener("pagehide", handlePageHide)
    window.removeEventListener("pageshow", handlePageShow)
    window.removeEventListener("blur", handleBlur)
    window.removeEventListener("focus", handleFocus)
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
    awayMs,
    awayCount,
  }
}
