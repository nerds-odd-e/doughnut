import type { NoteTopology } from "@generated/doughnut-backend-api"

/** Immediate parent first, then each ancestor toward the notebook root. */
export function ancestorTopologyIds(
  parentTopic: NoteTopology | undefined
): number[] {
  const ids: number[] = []
  let cursor = parentTopic
  while (cursor) {
    ids.push(cursor.id)
    cursor = cursor.parentOrSubjectNoteTopology
  }
  return ids
}
