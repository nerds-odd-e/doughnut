import { ref, onUnmounted, nextTick } from "vue"

export function useThinkingTimeTracker() {
  const accumulatedMs = ref(0)
  const runningStart = ref<number | null>(null)
  const isRunning = ref(false)
  const hasStopped = ref(false)
  const currentTimeMs = ref(0)
  let updateInterval: number | null = null

  const pause = () => {
    if (!isRunning.value || runningStart.value === null) return

    const now = performance.now()
    accumulatedMs.value += now - runningStart.value
    runningStart.value = null
    isRunning.value = false
  }

  const resume = () => {
    if (hasStopped.value || isRunning.value) return

    runningStart.value = performance.now()
    isRunning.value = true
  }

  const start = async () => {
    if (hasStopped.value) return

    await nextTick()
    requestAnimationFrame(() => {
      if (!hasStopped.value) {
        resume()
        startUpdateInterval()
      }
    })
  }

  const stop = (): number => {
    stopUpdateInterval()

    if (hasStopped.value) {
      return accumulatedMs.value
    }

    hasStopped.value = true

    if (isRunning.value && runningStart.value !== null) {
      const now = performance.now()
      accumulatedMs.value += now - runningStart.value
      runningStart.value = null
      isRunning.value = false
    }

    updateCurrentTime()
    return Math.round(accumulatedMs.value)
  }

  const handleVisibilityChange = () => {
    if (document.hidden) {
      pause()
    } else {
      resume()
    }
  }

  const handleBlur = () => {
    pause()
  }

  const handleFocus = () => {
    resume()
  }

  const updateCurrentTime = () => {
    if (hasStopped.value) {
      currentTimeMs.value = Math.round(accumulatedMs.value)
    } else if (isRunning.value && runningStart.value !== null) {
      currentTimeMs.value = Math.round(
        accumulatedMs.value + (performance.now() - runningStart.value)
      )
    } else {
      currentTimeMs.value = Math.round(accumulatedMs.value)
    }
  }

  const setupEventListeners = () => {
    document.addEventListener("visibilitychange", handleVisibilityChange)
    window.addEventListener("blur", handleBlur)
    window.addEventListener("focus", handleFocus)
  }

  const removeEventListeners = () => {
    document.removeEventListener("visibilitychange", handleVisibilityChange)
    window.removeEventListener("blur", handleBlur)
    window.removeEventListener("focus", handleFocus)
  }

  const startUpdateInterval = () => {
    if (updateInterval !== null) return
    updateInterval = window.setInterval(() => {
      updateCurrentTime()
    }, 100)
  }

  const stopUpdateInterval = () => {
    if (updateInterval !== null) {
      clearInterval(updateInterval)
      updateInterval = null
    }
  }

  setupEventListeners()

  onUnmounted(() => {
    removeEventListeners()
    stopUpdateInterval()
    pause()
  })

  return {
    start,
    stop,
    accumulatedMs,
    isRunning,
    currentTimeMs,
  }
}
