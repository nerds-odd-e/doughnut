import { TEXT_AUTOSAVE_DEBOUNCE_MS } from "@/composables/useDebouncedTextAutosave"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"

/** Matches note body / readmeContent auto-save debounce (`useDebouncedTextAutosave`). */
export const NOTE_CONTENT_SAVE_DEBOUNCE_MS = TEXT_AUTOSAVE_DEBOUNCE_MS

export async function advanceNoteContentSaveDebounce() {
  vi.advanceTimersByTime(NOTE_CONTENT_SAVE_DEBOUNCE_MS)
  await flushPromises()
}
