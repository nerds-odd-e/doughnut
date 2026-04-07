import { ref, onUnmounted, nextTick } from "vue"

export function useThinkingTimeTracker() {
  const accumulatedMs = ref(0)
  const runningStart = ref<number | null>(null)
  const isRunning = ref(false)
  const isPaused = ref(false)
  const hasStopped = ref(false)

  let visibilitySyncIntervalId: ReturnType<typeof setInterval> | null = null

  const clearVisibilitySync = () => {
    if (visibilitySyncIntervalId !== null) {
      clearInterval(visibilitySyncIntervalId)
      visibilitySyncIntervalId = null
    }
  }

  const pause = () => {
    if (!isRunning.value || runningStart.value === null) return

    const now = performance.now()
    accumulatedMs.value += now - runningStart.value
    runningStart.value = null
    isRunning.value = false
    isPaused.value = true
    clearVisibilitySync()
  }

  const resume = () => {
    if (hasStopped.value || isRunning.value) return
    if (document.hidden) return

    runningStart.value = performance.now()
    isRunning.value = true
    isPaused.value = false

    clearVisibilitySync()
    visibilitySyncIntervalId = setInterval(() => {
      if (hasStopped.value) {
        clearVisibilitySync()
        return
      }
      if (isRunning.value && document.hidden) {
        pause()
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
    if (isRunning.value && runningStart.value !== null) {
      const now = performance.now()
      accumulatedMs.value += now - runningStart.value
      runningStart.value = now
    }
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
      pause()
    } else {
      resume()
    }
  }

  const handlePageHide = () => {
    pause()
  }

  const handlePageShow = () => {
    resume()
  }

  const handleBlur = () => {
    pause()
  }

  const handleFocus = () => {
    if (document.hidden) return
    resume()
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
    clearVisibilitySync()
    pause()
  })

  return {
    start,
    stop,
    pause,
    resume,
    isRunning,
    isPaused,
  }
}
