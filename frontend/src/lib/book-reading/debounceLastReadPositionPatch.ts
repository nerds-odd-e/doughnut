import { debounce } from "es-toolkit"

export type LastReadPositionPatchBody = {
  pageIndex: number
  normalizedY: number
  selectedBookBlockId?: number
}

export type LastReadPositionPatchDebouncer = {
  propose: (
    pageIndex: number,
    normalizedY: number,
    selectedBookBlockId?: number
  ) => void
  cancel: () => void
}

export function createLastReadPositionPatchDebouncer(options: {
  delayMs: number
  patch: (body: LastReadPositionPatchBody) => Promise<unknown>
}): LastReadPositionPatchDebouncer {
  const { delayMs, patch } = options
  let lastSent: LastReadPositionPatchBody | null = null

  function same(a: LastReadPositionPatchBody, b: LastReadPositionPatchBody) {
    if (a.pageIndex !== b.pageIndex || a.normalizedY !== b.normalizedY) {
      return false
    }
    return a.selectedBookBlockId === b.selectedBookBlockId
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

  const debounced = debounce(
    (pageIndex: number, normalizedY: number, selectedBookBlockId?: number) => {
      const next: LastReadPositionPatchBody =
        selectedBookBlockId === undefined
          ? { pageIndex, normalizedY }
          : { pageIndex, normalizedY, selectedBookBlockId }
      sendIfNeeded(next)
    },
    delayMs
  )

  return {
    propose(
      pageIndex: number,
      normalizedY: number,
      selectedBookBlockId?: number
    ) {
      debounced(pageIndex, normalizedY, selectedBookBlockId)
    },
    cancel() {
      debounced.cancel()
    },
  }
}
