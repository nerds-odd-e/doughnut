import { debounce } from "es-toolkit"

export type ViewportCurrentAnchorDebouncer = {
  propose: (id: number | null) => void
  cancel: () => void
  commitNow: (id: number | null) => void
}

export function createViewportCurrentAnchorDebouncer(options: {
  delayMs: number
  commit: (id: number | null) => void
}): ViewportCurrentAnchorDebouncer {
  const { delayMs, commit } = options
  let lastCommitted: number | null = null

  const apply = (id: number | null) => {
    if (id === lastCommitted) {
      return
    }
    lastCommitted = id
    commit(id)
  }

  const debounced = debounce(apply, delayMs)

  return {
    propose(id: number | null) {
      debounced(id)
    },
    cancel() {
      debounced.cancel()
    },
    commitNow(id: number | null) {
      debounced.cancel()
      if (id === lastCommitted) {
        return
      }
      lastCommitted = id
      commit(id)
    },
  }
}
