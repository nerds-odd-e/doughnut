import {
  computeNoteToolbarOverflow,
  NOTE_TOOLBAR_MORE_OPTIONS_ORDER,
} from "@/composables/noteToolbarOverflow"
import type { NoteMoreOptionsActionId } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { describe, expect, it } from "vitest"

const presentIds = NOTE_TOOLBAR_MORE_OPTIONS_ORDER
const widths = {
  export: 40,
  questions: 40,
  audio: 40,
  assimilation: 40,
  delete: 40,
} as const
const overflowButtonWidth = 32
const allActionsWidth = 200

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

  it("omits from the right: delete, off assimilation, off audio, questions, export", () => {
    expect(overflow(152)).toEqual(["delete", "assimilation"])
    expect(overflow(151)).toEqual(["delete", "assimilation", "audio"])
    expect(overflow(111)).toEqual([
      "delete",
      "assimilation",
      "audio",
      "questions",
    ])
    expect(overflow(71)).toEqual([
      "delete",
      "assimilation",
      "audio",
      "questions",
      "export",
    ])
  })

  it("never omits a pinned id and still hides from the right around it", () => {
    expect(overflow(152, ["assimilation"])).toEqual(["delete", "audio"])
    expect(overflow(120, ["audio"])).toEqual([
      "delete",
      "assimilation",
      "questions",
    ])
  })

  it("keeps pinned ids even when they overflow the available width", () => {
    expect(overflow(0, ["audio"])).toEqual([
      "delete",
      "assimilation",
      "questions",
      "export",
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
