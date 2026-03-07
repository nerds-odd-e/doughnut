import type { UpdateNoteTypeData } from "@generated/doughnut-backend-api"

export type NoteType = UpdateNoteTypeData["body"]

export const noteTypeOptions: NoteType[] = [
  "concept",
  "source",
  "person",
  "experience",
  "initiative",
  "quest",
]
