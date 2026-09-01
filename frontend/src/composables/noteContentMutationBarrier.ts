type NoteContentAutosave = {
  flushAndWait: () => Promise<boolean>
}

type NoteMutationState = {
  admissionOpen: boolean
  autosave?: NoteContentAutosave
}

const noteMutations = new Map<number, NoteMutationState>()

function stateFor(noteId: number): NoteMutationState {
  const existing = noteMutations.get(noteId)
  if (existing) return existing
  const created = { admissionOpen: true }
  noteMutations.set(noteId, created)
  return created
}

export function registerNoteContentAutosave(
  noteId: number,
  autosave: NoteContentAutosave
): () => void {
  const state = stateFor(noteId)
  state.autosave = autosave
  return () => {
    if (state.autosave === autosave) {
      state.autosave = undefined
    }
    if (!state.autosave) {
      noteMutations.delete(noteId)
    }
  }
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
  const saved = (await state.autosave?.flushAndWait()) ?? true
  if (!saved) {
    reopenNoteContentMutations(noteId)
  }
  return saved
}

export function reopenNoteContentMutations(noteId: number): void {
  const state = noteMutations.get(noteId)
  if (!state) return
  state.admissionOpen = true
  if (!state.autosave) {
    noteMutations.delete(noteId)
  }
}
