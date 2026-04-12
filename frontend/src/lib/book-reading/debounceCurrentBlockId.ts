import { debounce } from "es-toolkit"
import { readonly, ref, type DeepReadonly, type Ref } from "vue"

export type CurrentBlockIdDebouncer = {
  currentBlockId: DeepReadonly<Ref<number | null>>
  propose: (id: number | null) => void
  cancel: () => void
  commitNow: (id: number | null) => void
}

export function createCurrentBlockIdDebouncer(options: {
  delayMs: number
  commit: (id: number | null) => boolean
}): CurrentBlockIdDebouncer {
  const { delayMs, commit } = options
  const currentBlockId = ref<number | null>(null)
  let lastCommitted: number | null = null

  const apply = (id: number | null) => {
    if (id === lastCommitted) {
      return
    }
    const prev = lastCommitted
    lastCommitted = id
    if (!commit(id)) {
      lastCommitted = prev
    } else {
      currentBlockId.value = id
    }
  }

  const debounced = debounce(apply, delayMs)

  return {
    currentBlockId: readonly(currentBlockId),
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
      } else {
        currentBlockId.value = id
      }
    },
  }
}
