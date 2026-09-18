export const noteToolbarEditTitles = {
  markdown: "Edit as markdown (m)",
  rich: "Edit as rich content (m)",
} as const

export const noteToolbarEditTitle = (asMarkdown?: boolean) =>
  asMarkdown ? noteToolbarEditTitles.rich : noteToolbarEditTitles.markdown

export const noteDeleteTitles = {
  trash: "Trash note (d)",
  permanent: "Permanently delete note (d)",
} as const

export const noteDeleteTitle = (trashed: boolean) =>
  trashed ? noteDeleteTitles.permanent : noteDeleteTitles.trash

export const noteMoreOptionsTitles = {
  new: "New note (n)",
  wiki: "Wiki link or relationship (Ctrl+Shift+F / Cmd+Shift+F)",
  conversation: "Start a conversation about this note",
  export: "Export... (e)",
  mcqs: "Questions for the note",
  refine: "Refine note",
  audio: "Audio tools",
  assimilation: "Assimilate",
  delete: noteDeleteTitles.trash,
  overflowMenu: "more options",
} as const

export type NoteMoreOptionsActionId =
  | Exclude<keyof typeof noteMoreOptionsTitles, "overflowMenu">
  | "edit"

export const noteToolbarOverflowTitles = (
  id: NoteMoreOptionsActionId
): readonly string[] => {
  if (id === "edit") return Object.values(noteToolbarEditTitles)
  if (id === "delete") return Object.values(noteDeleteTitles)
  return [noteMoreOptionsTitles[id]]
}
