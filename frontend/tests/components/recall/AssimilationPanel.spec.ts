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
  it("renders Recall modes without progress summary, Level radios, or Refine note trigger", async () => {
    const wrapper = await mountAssimilationPanelReady()

    const modes = wrapper.find('[data-testid="note-assimilation-modes"]')
    expect(modes.find("h2").text()).toBe("Recall modes")
    expect(
      modes.find('[data-test="assimilation-progress-summary"]').exists()
    ).toBe(false)
    expect(modes.find('[role="radiogroup"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="open-refine-note-modal"]').exists()).toBe(
      false
    )
    expect(document.querySelector('[data-test="refine-note-modal"]')).toBeNull()
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
