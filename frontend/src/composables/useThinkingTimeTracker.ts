import { ref, onUnmounted, nextTick } from "vue"

export function useThinkingTimeTracker() {
  const accumulatedMs = ref(0)
  const runningStart = ref<number | null>(null)
  const isRunning = ref(false)
  const hasStopped = ref(false)

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

  const handleBlur = () => {
    pause()
  }

  const handleFocus = () => {
    resume()
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

  setupEventListeners()

  onUnmounted(() => {
    removeEventListeners()
    pause()
  })

  return {
    start,
    stop,
    updateAccumulatedTime,
  }
}
