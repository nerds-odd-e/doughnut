import { ref } from "vue"

/** Module-level singleton: holds the inserter registered by the currently mounted NoteEditableDetails. */
const _inserter = ref<((text: string) => void) | undefined>(undefined)

export function useDetailsCursorInserter() {
  function registerInserter(fn: (text: string) => void) {
    _inserter.value = fn
  }

  function unregisterInserter() {
    _inserter.value = undefined
  }

  function insert(text: string) {
    if (_inserter.value) {
      _inserter.value(text)
    }
  }

  return { registerInserter, unregisterInserter, insert }
}
