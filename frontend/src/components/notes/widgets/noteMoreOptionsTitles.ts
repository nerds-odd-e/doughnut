export const noteToolbarEditTitles = {
  markdown: "Edit as markdown (m)",
  rich: "Edit as rich content (m)",
} as const

export const noteToolbarEditTitle = (asMarkdown?: boolean) =>
  asMarkdown ? noteToolbarEditTitles.rich : noteToolbarEditTitles.markdown

export const noteMoreOptionsTitles = {
  new: "New note (n)",
  wiki: "Wiki link or relationship (Ctrl+Shift+F / Cmd+Shift+F)",
  conversation: "Star a conversation about this note",
  export: "Export... (e)",
  questions: "Questions for the note",
  audio: "Audio tools",
  assimilation: "Assimilation settings",
  delete: "Delete note (d)",
  overflowMenu: "more options",
} as const

export type NoteMoreOptionsActionId =
  | Exclude<keyof typeof noteMoreOptionsTitles, "overflowMenu">
  | "edit"

export const noteToolbarOverflowTitles = (
  id: NoteMoreOptionsActionId
): readonly string[] =>
  id === "edit"
    ? Object.values(noteToolbarEditTitles)
    : [noteMoreOptionsTitles[id]]
