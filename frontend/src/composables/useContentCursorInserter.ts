import { ref } from "vue"

/** Module-level singleton: holds the inserter registered by the currently mounted NoteEditableContent. */
const _inserter = ref<((text: string) => void) | undefined>(undefined)

export type InsertWikiLinkAsPropertyInserter = {
  canInsert: () => boolean
  insert: (text: string) => void
}

const _insertWikiLinkAsPropertyInserter = ref<
  InsertWikiLinkAsPropertyInserter | undefined
>(undefined)

export function useContentCursorInserter() {
  function registerInserter(fn: (text: string) => void) {
    _inserter.value = fn
  }

  function registerInsertWikiLinkAsPropertyInserter(
    reg: InsertWikiLinkAsPropertyInserter
  ) {
    _insertWikiLinkAsPropertyInserter.value = reg
  }

  function unregisterInserter() {
    _inserter.value = undefined
    _insertWikiLinkAsPropertyInserter.value = undefined
  }

  function insert(text: string) {
    if (_inserter.value) {
      _inserter.value(text)
    }
  }

  function canInsertWikiLinkAsProperty(): boolean {
    return _insertWikiLinkAsPropertyInserter.value?.canInsert() ?? false
  }

  function insertWikiLinkAsProperty(text: string) {
    _insertWikiLinkAsPropertyInserter.value?.insert(text)
  }

  return {
    registerInserter,
    registerInsertWikiLinkAsPropertyInserter,
    unregisterInserter,
    insert,
    canInsertWikiLinkAsProperty,
    insertWikiLinkAsProperty,
  }
}
