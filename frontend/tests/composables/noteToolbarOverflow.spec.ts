import {
  computeNoteToolbarOverflow,
  NOTE_TOOLBAR_MORE_OPTIONS_ORDER,
} from "@/composables/noteToolbarOverflow"
import type { NoteMoreOptionsActionId } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { describe, expect, it } from "vitest"

const presentIds = NOTE_TOOLBAR_MORE_OPTIONS_ORDER
const widths = {
  new: 40,
  wiki: 40,
  conversation: 40,
  edit: 40,
  export: 40,
  mcqs: 40,
  refine: 40,
  audio: 40,
  assimilation: 40,
  delete: 40,
} as const
const overflowButtonWidth = 32
const allActionsWidth = presentIds.reduce((sum, id) => sum + widths[id], 0)
const remainingPlusOverflow = (count: number) =>
  count * 40 + overflowButtonWidth

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

  it("omits from the right: conversation before wiki before new", () => {
    expect(overflow(remainingPlusOverflow(4))).toEqual([
      "delete",
      "assimilation",
      "audio",
      "refine",
      "mcqs",
      "export",
    ])
    expect(overflow(remainingPlusOverflow(3))).toEqual([
      "delete",
      "assimilation",
      "audio",
      "refine",
      "mcqs",
      "export",
      "edit",
    ])
    expect(overflow(remainingPlusOverflow(2))).toEqual([
      "delete",
      "assimilation",
      "audio",
      "refine",
      "mcqs",
      "export",
      "edit",
      "conversation",
    ])
    expect(overflow(remainingPlusOverflow(1))).toEqual([
      "delete",
      "assimilation",
      "audio",
      "refine",
      "mcqs",
      "export",
      "edit",
      "conversation",
      "wiki",
    ])
    expect(overflow(remainingPlusOverflow(0))).toEqual([
      "delete",
      "assimilation",
      "audio",
      "refine",
      "mcqs",
      "export",
      "edit",
      "conversation",
      "wiki",
      "new",
    ])
  })

  it("never omits a pinned id and still hides from the right around it", () => {
    expect(overflow(remainingPlusOverflow(3), ["assimilation"])).toEqual([
      "delete",
      "audio",
      "refine",
      "mcqs",
      "export",
      "edit",
      "conversation",
    ])
    expect(overflow(remainingPlusOverflow(2), ["audio"])).toEqual([
      "delete",
      "assimilation",
      "refine",
      "mcqs",
      "export",
      "edit",
      "conversation",
      "wiki",
    ])
  })

  it("keeps pinned ids even when they overflow the available width", () => {
    expect(overflow(0, ["audio"])).toEqual([
      "delete",
      "assimilation",
      "refine",
      "mcqs",
      "export",
      "edit",
      "conversation",
      "wiki",
      "new",
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
