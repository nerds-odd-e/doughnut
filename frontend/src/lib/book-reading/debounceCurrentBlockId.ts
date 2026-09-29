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
}): CurrentBlockIdDebouncer {
  const currentBlockId = ref<number | null>(null)

  const commit = (id: number | null) => {
    currentBlockId.value = id
  }

  const debounced = debounce(commit, options.delayMs)

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
      commit(id)
    },
  }
}
