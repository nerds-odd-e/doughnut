import type { TitleDictation } from "@/composables/useTitleDictation"
import { FIELD_JOIN_APPEND_BUTTON_CLASS } from "@/utils/fieldJoinAppendButtonClass"

export const DEFAULT_NEW_NOTE_TITLE = "Untitled"

export function initialNewNoteTitle(initialTitle?: string): string {
  if (initialTitle === undefined) return DEFAULT_NEW_NOTE_TITLE
  return initialTitle.endsWith(" ") ? initialTitle : `${initialTitle} `
}

/** An untouched default is a placeholder: the whole of it is the target heard words replace. Any other title takes them at its caret or selection. */
export function wholeTitleIsDictationTarget(
  hasTitleBeenEdited: boolean,
  currentTitle: string
): boolean {
  return !hasTitleBeenEdited && currentTitle === DEFAULT_NEW_NOTE_TITLE
}

export const speakTitleNames = {
  start: "Speak the title",
  stop: "Stop speaking the title",
} as const

/** What the title field's microphone button shows in each phase of a title recording. */
export function speakTitleButtonState(
  phase: TitleDictation["voiceInput"]["phase"]["value"]
) {
  if (phase === "listening") return "recording"
  return phase === "idle" ? "idle" : "busy"
}

/** The title field's frame for the microphone button: highlighted while recording. */
export function speakTitleButtonClass(
  state: ReturnType<typeof speakTitleButtonState>
) {
  return `${
    state === "recording"
      ? "daisy-btn daisy-join-item daisy-btn-soft daisy-btn-primary shrink-0"
      : FIELD_JOIN_APPEND_BUTTON_CLASS
  } rounded-none`
}
