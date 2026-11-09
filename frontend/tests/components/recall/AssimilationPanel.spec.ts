import {
  AssimilationSequenceSkipController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import {
  assimilateButtonEl,
  assimilateSpy,
  assimilatedCountOfTheDay,
  clickAssimilate,
  clickReturnToSequence,
  clickSkipAndConfirm,
  dueRecallsRefreshRequested,
  expectAtNoteOf,
  totalAssimilatedCount,
  mountAssimilationPanelReady,
  nextNoteId,
  note,
  returnToSequenceButtonEl,
  setupAssimilationPanelTests,
  skipButtonEl,
  skipSequenceSpy,
  understandingStatusSelector,
} from "./assimilationPanelTestSupport"

setupAssimilationPanelTests()

describe("AssimilationPanel", () => {
  it("offers Understanding as the one primary action and each optional mode with its icon, description and own action", async () => {
    const wrapper = await mountAssimilationPanelReady()

    const texts = (mode: string, selector: string) =>
      wrapper
        .findAll(`[data-test="assimilation-mode-row-${mode}"] :is(${selector})`)
        .map((el) => el.text())
    expect(wrapper.findAll(".daisy-btn-primary").map((b) => b.text())).toEqual([
      "Assimilate understanding",
    ])
    expect(wrapper.text()).toMatch(/Default mode.*Optional modes/)
    expect(texts("UNDERSTANDING", "h4, p, button")).toEqual([
      "Understanding",
      "Recall what this note means.",
      "Assimilate understanding",
      "Skip from sequence…",
    ])
    expect(texts("SPELLING", "strong, p, button")).toEqual([
      "Spelling",
      "Recall the title or an alias from memory.",
      "Verify spelling",
    ])
    expect(texts("COMMISSIONED", "strong, p, button")).toEqual([
      "Commissioned",
      "Review in future learning sessions with a human or AI tutor.",
      "Add to sessions",
    ])
    for (const [mode, icon] of [
      ["SPELLING", "lucide-spell-check"],
      ["COMMISSIONED", "lucide-graduation-cap"],
    ]) {
      expect(
        wrapper
          .find(`[data-test="assimilation-mode-row-${mode}"] svg`)
          .classes()
      ).toContain(icon)
    }
  })

  it("advances via next assimilation and increments counts when assimilating", async () => {
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([makeMe.aMemoryTracker.id(1).please()])
    )
    const wrapper = await mountAssimilationPanelReady()

    await clickAssimilate(wrapper)

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: { noteId: note.id },
    })
    expectAtNoteOf(nextNoteId)
    expect(totalAssimilatedCount.value).toBe(1)
    expect(assimilatedCountOfTheDay.value).toBe(1)
    expect(dueRecallsRefreshRequested()).toBe(true)
  })

  it("skips the sequence without creating a tracker or incrementing daily count", async () => {
    const wrapper = await mountAssimilationPanelReady()

    await clickSkipAndConfirm(wrapper)

    expect(skipSequenceSpy).toHaveBeenCalledWith({
      body: { noteId: note.id },
    })
    expect(assimilateSpy).not.toHaveBeenCalled()
    expectAtNoteOf(nextNoteId)
    expect(totalAssimilatedCount.value).toBe(0)
    expect(assimilatedCountOfTheDay.value).toBe(0)
    expect(dueRecallsRefreshRequested()).toBe(false)
  })

  it("shows Return to sequence instead of Skip when the note is sequence-skipped", async () => {
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo.skippedPropertyKeys([""]).please()
    )
    const wrapper = await mountAssimilationPanelReady()

    expect(returnToSequenceButtonEl(wrapper)).not.toBeNull()
    expect(skipButtonEl(wrapper)).toBeNull()
  })

  it("returns the note to the sequence without creating a tracker", async () => {
    let skippedKeys = [""]
    mockSdkServiceWithImplementation(NoteController, "getNoteInfo", () =>
      makeMe.aNoteRecallInfo.skippedPropertyKeys(skippedKeys).please()
    )
    const deleteSkipSpy = mockSdkService(
      AssimilationSequenceSkipController,
      "deleteAssimilationSequenceSkip",
      undefined as never
    )
    deleteSkipSpy.mockImplementation(async () => {
      skippedKeys = []
      return wrapSdkResponse(undefined)
    })
    const wrapper = await mountAssimilationPanelReady()

    await clickReturnToSequence(wrapper)

    expect(deleteSkipSpy).toHaveBeenCalledWith({
      body: { noteId: note.id },
    })
    expect(assimilateSpy).not.toHaveBeenCalled()
    expectAtNoteOf(note.id)
    expect(skipButtonEl(wrapper)).not.toBeNull()
    expect(returnToSequenceButtonEl(wrapper)).toBeNull()
  })

  it("shows the tracker status instead of Assimilate after a note-level assimilate creates an understanding tracker", async () => {
    let getNoteInfoCallCount = 0
    mockSdkServiceWithImplementation(NoteController, "getNoteInfo", () => {
      getNoteInfoCallCount += 1
      if (getNoteInfoCallCount === 1) {
        return { memoryTrackers: [] }
      }
      return {
        memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
      }
    })
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([makeMe.aMemoryTracker.id(1).please()])
    )

    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(false)

    await clickAssimilate(wrapper)

    expectAtNoteOf(nextNoteId)
    expect(assimilateButtonEl(wrapper)).toBeNull()
    expect(
      wrapper.element.querySelector(understandingStatusSelector)
    ).not.toBeNull()
  })
})
