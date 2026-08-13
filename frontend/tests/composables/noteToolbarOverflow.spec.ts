import {
  computeNoteToolbarOverflow,
  NOTE_TOOLBAR_MORE_OPTIONS_ORDER,
} from "@/composables/noteToolbarOverflow"
import type { NoteMoreOptionsActionId } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { describe, expect, it } from "vitest"

const presentIds = NOTE_TOOLBAR_MORE_OPTIONS_ORDER
const widths = {
  edit: 40,
  export: 40,
  questions: 40,
  audio: 40,
  assimilation: 40,
  delete: 40,
} as const
const overflowButtonWidth = 32
const allActionsWidth = presentIds.reduce((sum, id) => sum + widths[id], 0)
const editBesideOverflow = widths.edit + overflowButtonWidth

function overflow(
  availableWidth: number,
  pinnedIds: readonly NoteMoreOptionsActionId[] = []
) {
  return computeNoteToolbarOverflow({
    presentIds,
    pinnedIds,
    widthById: widths,
    overflowButtonWidth,
    availableWidth,
  })
}

describe("computeNoteToolbarOverflow", () => {
  it("omits nothing when every present action fits without the overflow button", () => {
    expect(overflow(allActionsWidth)).toEqual([])
  })

  it("omits delete first when the full set no longer fits", () => {
    expect(overflow(allActionsWidth - 1)).toEqual(["delete"])
  })

  it("includes overflow-button width once anything is omitted", () => {
    const remainingAfterDelete = allActionsWidth - widths.delete
    expect(overflow(remainingAfterDelete + overflowButtonWidth)).toEqual([
      "delete",
    ])
    expect(overflow(remainingAfterDelete + overflowButtonWidth - 1)).toEqual([
      "delete",
      "assimilation",
    ])
  })

  it("omits from the right: delete, off assimilation, off audio, questions, export, then edit", () => {
    expect(overflow(192)).toEqual(["delete", "assimilation"])
    expect(overflow(191)).toEqual(["delete", "assimilation", "audio"])
    expect(overflow(151)).toEqual([
      "delete",
      "assimilation",
      "audio",
      "questions",
    ])
    expect(overflow(editBesideOverflow)).toEqual([
      "delete",
      "assimilation",
      "audio",
      "questions",
      "export",
    ])
    expect(overflow(editBesideOverflow - 1)).toEqual([
      "delete",
      "assimilation",
      "audio",
      "questions",
      "export",
      "edit",
    ])
  })

  it("never omits a pinned id and still hides from the right around it", () => {
    expect(overflow(152, ["assimilation"])).toEqual([
      "delete",
      "audio",
      "questions",
    ])
    expect(overflow(120, ["audio"])).toEqual([
      "delete",
      "assimilation",
      "questions",
      "export",
    ])
  })

  it("keeps pinned ids even when they overflow the available width", () => {
    expect(overflow(0, ["audio"])).toEqual([
      "delete",
      "assimilation",
      "questions",
      "export",
      "edit",
    ])
  })

  it("treats missing widths as zero", () => {
    expect(
      computeNoteToolbarOverflow({
        presentIds: ["export", "delete"],
        pinnedIds: [],
        widthById: { export: 40 },
        overflowButtonWidth: 10,
        availableWidth: 40,
      })
    ).toEqual([])
  })
})
