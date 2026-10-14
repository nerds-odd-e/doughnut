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
