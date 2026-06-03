import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"

/** Matches `NoteEditableContent` auto-save debounce interval. */
export const NOTE_CONTENT_SAVE_DEBOUNCE_MS = 1000

export async function advanceNoteContentSaveDebounce() {
  vi.advanceTimersByTime(NOTE_CONTENT_SAVE_DEBOUNCE_MS)
  await flushPromises()
}
