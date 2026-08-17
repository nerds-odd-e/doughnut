export function initialNewNoteTitle(initialTitle?: string): string {
  if (initialTitle === undefined) return "Untitled"
  return initialTitle.endsWith(" ") ? initialTitle : `${initialTitle} `
}
