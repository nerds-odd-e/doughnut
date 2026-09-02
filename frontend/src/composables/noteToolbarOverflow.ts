import type { NoteMoreOptionsActionId } from "@/components/notes/widgets/noteMoreOptionsTitles"

export const NOTE_TOOLBAR_MORE_OPTIONS_ORDER = [
  "new",
  "wiki",
  "conversation",
  "edit",
  "export",
  "mcqs",
  "audio",
  "assimilation",
  "delete",
] as const satisfies readonly NoteMoreOptionsActionId[]

export type NoteToolbarOverflowInput = {
  presentIds: readonly NoteMoreOptionsActionId[]
  pinnedIds: readonly NoteMoreOptionsActionId[]
  widthById: Readonly<Partial<Record<NoteMoreOptionsActionId, number>>>
  overflowButtonWidth: number
  availableWidth: number
}

const widthOf = (
  widthById: NoteToolbarOverflowInput["widthById"],
  id: NoteMoreOptionsActionId
) => widthById[id] ?? 0

const fits = (
  input: NoteToolbarOverflowInput,
  omit: ReadonlySet<NoteMoreOptionsActionId>,
  includeOverflowButton: boolean
) => {
  let used = includeOverflowButton ? input.overflowButtonWidth : 0
  for (const id of input.presentIds) {
    if (omit.has(id)) continue
    used += widthOf(input.widthById, id)
  }
  return used <= input.availableWidth
}

export function computeNoteToolbarOverflow(
  input: NoteToolbarOverflowInput
): NoteMoreOptionsActionId[] {
  const pinned = new Set(input.pinnedIds)
  if (fits(input, new Set(), false)) return []

  const omit: NoteMoreOptionsActionId[] = []
  const omitSet = new Set<NoteMoreOptionsActionId>()

  for (let index = input.presentIds.length - 1; index >= 0; index -= 1) {
    const id = input.presentIds[index]
    if (id === undefined || pinned.has(id)) continue
    omit.push(id)
    omitSet.add(id)
    if (fits(input, omitSet, true)) return omit
  }

  return omit
}
