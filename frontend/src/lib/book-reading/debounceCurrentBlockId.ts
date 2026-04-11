import { debounce } from "es-toolkit"

export type CurrentBlockIdDebouncer = {
  propose: (id: number | null) => void
  cancel: () => void
  commitNow: (id: number | null) => void
}

export function createCurrentBlockIdDebouncer(options: {
  delayMs: number
  commit: (id: number | null) => boolean
}): CurrentBlockIdDebouncer {
  const { delayMs, commit } = options
  let lastCommitted: number | null = null

  const apply = (id: number | null) => {
    if (id === lastCommitted) {
      return
    }
    const prev = lastCommitted
    lastCommitted = id
    if (!commit(id)) {
      lastCommitted = prev
    }
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
      const prev = lastCommitted
      lastCommitted = id
      if (!commit(id)) {
        lastCommitted = prev
      }
    },
  }
}
