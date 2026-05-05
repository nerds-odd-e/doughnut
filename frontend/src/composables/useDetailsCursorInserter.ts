import { ref } from "vue"

/** Module-level singleton: holds the inserter registered by the currently mounted NoteEditableDetails. */
const _inserter = ref<((text: string) => void) | undefined>(undefined)

export type WikiPropertyInserter = {
  canInsert: () => boolean
  insert: (text: string) => void
}

const _wikiPropertyInserter = ref<WikiPropertyInserter | undefined>(undefined)

export function useDetailsCursorInserter() {
  function registerInserter(fn: (text: string) => void) {
    _inserter.value = fn
  }

  function registerWikiPropertyInserter(reg: WikiPropertyInserter) {
    _wikiPropertyInserter.value = reg
  }

  function unregisterInserter() {
    _inserter.value = undefined
    _wikiPropertyInserter.value = undefined
  }

  function insert(text: string) {
    if (_inserter.value) {
      _inserter.value(text)
    }
  }

  function canInsertWikiLinkAsProperty(): boolean {
    return _wikiPropertyInserter.value?.canInsert() ?? false
  }

  function insertWikiLinkAsProperty(text: string) {
    _wikiPropertyInserter.value?.insert(text)
  }

  return {
    registerInserter,
    registerWikiPropertyInserter,
    unregisterInserter,
    insert,
    canInsertWikiLinkAsProperty,
    insertWikiLinkAsProperty,
  }
}
