import { TEXT_AUTOSAVE_DEBOUNCE_MS } from "@/composables/useDebouncedTextAutosave"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"

/** Matches `useDebouncedTextAutosave` delay. */
export const NOTE_CONTENT_SAVE_DEBOUNCE_MS = TEXT_AUTOSAVE_DEBOUNCE_MS

export async function advanceNoteContentSaveDebounce() {
  vi.advanceTimersByTime(NOTE_CONTENT_SAVE_DEBOUNCE_MS)
  await flushPromises()
}

/** Promise with an external resolve for gating in-flight autosave responses. */
export function deferred() {
  let resolve!: () => void
  const promise = new Promise<void>((done) => {
    resolve = done
  })
  return { promise, resolve }
}
