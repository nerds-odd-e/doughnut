import { onUnmounted, watch } from "vue"
import type { DictationTarget } from "@/models/audio/dictationTarget"

type OpenNoteContentEditor = {
  /** Saves the draft right away. */
  flush: () => void
  flushAndWait: () => Promise<boolean>
  beginDictation: () => DictationTarget
}

type NoteMutationState = {
  admissionOpen: boolean
  editor?: OpenNoteContentEditor
  dictation?: DictationTarget
}

const noteMutations = new Map<number, NoteMutationState>()

function stateFor(noteId: number): NoteMutationState {
  const existing = noteMutations.get(noteId)
  if (existing) return existing
  const created = { admissionOpen: true }
  noteMutations.set(noteId, created)
  return created
}

function registerOpenNoteContentEditor(
  noteId: number,
  editor: OpenNoteContentEditor
): () => void {
  const state = stateFor(noteId)
  state.editor = editor
  return () => {
    if (state.editor === editor) {
      state.editor = undefined
      state.dictation?.end(false)
      state.dictation = undefined
    }
    if (!state.editor) {
      noteMutations.delete(noteId)
    }
  }
}

/** Keeps the component's body editor registered as the open one of the note it shows. */
export function useOpenNoteContentEditor(
  noteId: () => number,
  editor: OpenNoteContentEditor
): void {
  let unregister: (() => void) | undefined
  watch(
    noteId,
    (id) => {
      unregister?.()
      unregister = registerOpenNoteContentEditor(id, editor)
    },
    { immediate: true }
  )
  onUnmounted(() => unregister?.())
}

/** Begins a dictation session in the note's open body editor, when one is open; its insertions are saved right away. */
export function beginOpenNoteContentDictation(noteId: number): void {
  const state = noteMutations.get(noteId)
  if (!state?.editor) return
  const editor = state.editor
  const target = editor.beginDictation()
  state.dictation = {
    insert: (segments) => {
      target.insert(segments)
      editor.flush()
    },
    end: target.end,
  }
}

/** Puts the segments at the session's target and saves them; false when no session is open in an editor or admission is closed. */
export function insertDictationInOpenNoteContent(
  noteId: number,
  segments: readonly string[]
): boolean {
  const state = noteMutations.get(noteId)
  if (!state?.dictation || !state.admissionOpen) return false
  state.dictation.insert(segments)
  return true
}

export function endOpenNoteContentDictation(noteId: number): void {
  const state = noteMutations.get(noteId)
  state?.dictation?.end(true)
  if (state) state.dictation = undefined
}

export function noteContentMutationAdmissionIsOpen(noteId: number): boolean {
  return noteMutations.get(noteId)?.admissionOpen ?? true
}

export async function closeAndFlushNoteContentMutations(
  noteId: number
): Promise<boolean> {
  const state = noteMutations.get(noteId)
  if (!state) return true
  state.admissionOpen = false
  const saved = (await state.editor?.flushAndWait()) ?? true
  if (!saved) {
    reopenNoteContentMutations(noteId)
  }
  return saved
}

export function reopenNoteContentMutations(noteId: number): void {
  const state = noteMutations.get(noteId)
  if (!state) return
  state.admissionOpen = true
  if (!state.editor) {
    noteMutations.delete(noteId)
  }
}
