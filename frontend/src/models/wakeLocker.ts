export interface WakeLocker {
  request: () => Promise<void>
  release: () => Promise<void>
}

export const createWakeLocker = (): WakeLocker => {
  let lock: WakeLockSentinel | null = null

  return {
    request: async function (): Promise<void> {
      if ("wakeLock" in navigator) {
        try {
          lock = await navigator.wakeLock.request("screen")
        } catch (err) {
          console.error(`Failed to request Wake Lock: ${err}`)
        }
      }
    },

    release: async function (): Promise<void> {
      if (lock) {
        try {
          await lock.release()
          lock = null
        } catch (err) {
          console.error(`Failed to release Wake Lock: ${err}`)
        }
      }
    },
  }
}
