import { debounce } from "es-toolkit"

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
  let lastSent: LastReadPositionPatchBody | null = null

  function same(a: LastReadPositionPatchBody, b: LastReadPositionPatchBody) {
    return a.pageIndex === b.pageIndex && a.normalizedY === b.normalizedY
  }

  const sendIfNeeded = (next: LastReadPositionPatchBody) => {
    if (lastSent !== null && same(lastSent, next)) {
      return
    }
    patch(next)
      .then(() => {
        lastSent = next
      })
      .catch(() => undefined)
  }

  const debounced = debounce((pageIndex: number, normalizedY: number) => {
    sendIfNeeded({ pageIndex, normalizedY })
  }, delayMs)

  return {
    propose(pageIndex: number, normalizedY: number) {
      debounced(pageIndex, normalizedY)
    },
    cancel() {
      debounced.cancel()
    },
  }
}
