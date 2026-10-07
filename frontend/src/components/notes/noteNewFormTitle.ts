import { joinDictatedSegments } from "@/models/audio/joinDictatedSegments"

export const DEFAULT_NEW_NOTE_TITLE = "Untitled"

export function initialNewNoteTitle(initialTitle?: string): string {
  if (initialTitle === undefined) return DEFAULT_NEW_NOTE_TITLE
  return initialTitle.endsWith(" ") ? initialTitle : `${initialTitle} `
}

/** Join heard segments onto the current title, replacing an untouched default. */
export function titleAfterSpokenSegments(
  hasTitleBeenEdited: boolean,
  currentTitle: string,
  segments: readonly string[]
): string {
  const replaceUntouchedDefault =
    !hasTitleBeenEdited && currentTitle === DEFAULT_NEW_NOTE_TITLE
  return joinDictatedSegments(
    replaceUntouchedDefault ? "" : currentTitle,
    segments
  )
}
