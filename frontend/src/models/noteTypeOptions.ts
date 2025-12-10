import type { UpdateNoteTypeData } from "@generated/backend"

export type NoteType = UpdateNoteTypeData["body"]

export const noteTypeOptions: NoteType[] = [
  "unassigned",
  "concept",
  "category",
  "vocab",
  "journal",
]
