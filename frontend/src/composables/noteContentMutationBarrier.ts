type OpenNoteContentEditor = {
  flushAndWait: () => Promise<boolean>
  appendToDraft: (text: string) => void
}

type NoteMutationState = {
  admissionOpen: boolean
  editor?: OpenNoteContentEditor
}

const noteMutations = new Map<number, NoteMutationState>()

function stateFor(noteId: number): NoteMutationState {
  const existing = noteMutations.get(noteId)
  if (existing) return existing
  const created = { admissionOpen: true }
  noteMutations.set(noteId, created)
  return created
}

export function registerOpenNoteContentEditor(
  noteId: number,
  editor: OpenNoteContentEditor
): () => void {
  const state = stateFor(noteId)
  state.editor = editor
  return () => {
    if (state.editor === editor) {
      state.editor = undefined
    }
    if (!state.editor) {
      noteMutations.delete(noteId)
    }
  }
}

/** Appends to the open body editor's draft for the note and saves it; false when none is open or admission is closed. */
export function appendToOpenNoteContentDraft(
  noteId: number,
  text: string
): boolean {
  const state = noteMutations.get(noteId)
  if (!state?.editor || !state.admissionOpen) return false
  state.editor.appendToDraft(text)
  return true
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
