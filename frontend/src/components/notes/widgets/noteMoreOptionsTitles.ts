export const noteMoreOptionsTitles = {
  export: "Export... (e)",
  questions: "Questions for the note",
  audio: "Audio tools",
  assimilation: "Assimilation settings",
  delete: "Delete note (d)",
  overflowMenu: "more options",
} as const

export type NoteMoreOptionsActionId = Exclude<
  keyof typeof noteMoreOptionsTitles,
  "overflowMenu"
>
