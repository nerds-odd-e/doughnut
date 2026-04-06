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
  let timeoutId: ReturnType<typeof setTimeout> | null = null
  /** `undefined` = nothing queued; `null` is a valid queued value */
  let pending: number | null | undefined
  let lastCommitted: number | null = null

  function fire() {
    timeoutId = null
    if (pending === undefined) return
    const next = pending
    pending = undefined
    if (next === lastCommitted) return
    lastCommitted = next
    commit(next)
  }

  return {
    propose(id: number | null) {
      if (id === lastCommitted && pending === undefined && timeoutId === null) {
        return
      }
      pending = id
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
    commitNow(id: number | null) {
      if (timeoutId !== null) {
        clearTimeout(timeoutId)
        timeoutId = null
      }
      pending = undefined
      if (id === lastCommitted) {
        return
      }
      lastCommitted = id
      commit(id)
    },
  }
}
