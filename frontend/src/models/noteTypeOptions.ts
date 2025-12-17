import type { UpdateNoteTypeData } from "@generated/backend"

export type NoteType = UpdateNoteTypeData["body"]

export const noteTypeOptions: NoteType[] = [
  "concept",
  "source",
  "person",
  "experience",
  "initiative",
  "quest",
]
