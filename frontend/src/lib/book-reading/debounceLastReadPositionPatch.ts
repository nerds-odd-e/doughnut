export type LastReadPositionPatchBody = {
  pageIndex: number
  normalizedY: number
}

export type LastReadPositionPatchDebouncer = {
  propose: (pageIndex: number, normalizedY: number) => void
  cancel: () => void
}

export function createLastReadPositionPatchDebouncer(options: {
  delayMs: number
  patch: (body: LastReadPositionPatchBody) => Promise<unknown>
}): LastReadPositionPatchDebouncer {
  const { delayMs, patch } = options
  let timeoutId: ReturnType<typeof setTimeout> | null = null
  let pending: LastReadPositionPatchBody | undefined
  let lastSent: LastReadPositionPatchBody | null = null

  function same(a: LastReadPositionPatchBody, b: LastReadPositionPatchBody) {
    return a.pageIndex === b.pageIndex && a.normalizedY === b.normalizedY
  }

  function fire() {
    timeoutId = null
    if (pending === undefined) return
    const next = pending
    pending = undefined
    if (lastSent !== null && same(lastSent, next)) {
      return
    }
    patch(next)
      .then(() => {
        lastSent = next
      })
      .catch(() => undefined)
  }

  return {
    propose(pageIndex, normalizedY) {
      const next = { pageIndex, normalizedY }
      if (
        lastSent !== null &&
        same(lastSent, next) &&
        pending === undefined &&
        timeoutId === null
      ) {
        return
      }
      pending = next
      if (timeoutId !== null) {
        clearTimeout(timeoutId)
      }
      timeoutId = setTimeout(fire, delayMs)
    },
    cancel() {
      if (timeoutId !== null) {
        clearTimeout(timeoutId)
        timeoutId = null
      }
      pending = undefined
    },
  }
}
