import {
  AssimilationSequenceSkipController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateButtonEl,
  assimilateSpy,
  assimilatedCountOfTheDay,
  clickAssimilate,
  clickReturnToSequence,
  clickSkipAndConfirm,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  mountAssimilationPanelReady,
  note,
  returnToSequenceButtonEl,
  setupAssimilationPanelTests,
  skipButtonEl,
  skipSequenceSpy,
  understandingStatusSelector,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel", () => {
  it("labels the choices as Recall modes without a progress summary", async () => {
    const wrapper = await mountAssimilationPanelReady()

    const modes = wrapper.find('[data-testid="note-assimilation-modes"]')
    expect(modes.find("h2").text()).toBe("Recall modes")
    expect(
      modes.find('[data-test="assimilation-progress-summary"]').exists()
    ).toBe(false)
  })

  it("does not show Level radios in assimilation modes", async () => {
    const wrapper = await mountAssimilationPanelReady()

    expect(
      wrapper
        .find('[data-testid="note-assimilation-modes"] [role="radiogroup"]')
        .exists()
    ).toBe(false)
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
    expect(mockedGoToNextAssimilation).toHaveBeenCalled()
    expect(mockedTotalAssimilatedCount.value).toBe(1)
    expect(assimilatedCountOfTheDay.value).toBe(1)
    expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
  })

  it("skips the sequence without creating a tracker or incrementing daily count", async () => {
    const wrapper = await mountAssimilationPanelReady()

    await clickSkipAndConfirm(wrapper)

    expect(skipSequenceSpy).toHaveBeenCalledWith({
      body: { noteId: note.id },
    })
    expect(assimilateSpy).not.toHaveBeenCalled()
    expect(mockedGoToNextAssimilation).toHaveBeenCalled()
    expect(mockedTotalAssimilatedCount.value).toBe(0)
    expect(assimilatedCountOfTheDay.value).toBe(0)
    expect(mockedRequestDueRecallsRefresh).not.toHaveBeenCalled()
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
    expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
    expect(skipButtonEl(wrapper)).not.toBeNull()
    expect(returnToSequenceButtonEl(wrapper)).toBeNull()
  })

  it("no longer renders a Refine note trigger in assimilation settings", async () => {
    const wrapper = await mountAssimilationPanelReady()

    expect(wrapper.find('[data-test="open-refine-note-modal"]').exists()).toBe(
      false
    )
    expect(document.querySelector('[data-test="refine-note-modal"]')).toBeNull()
  })

  describe("assimilate when note has memory trackers", () => {
    it("enables assimilate when note has only a property memory tracker", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        memoryTrackers: [
          makeMe.aMemoryTracker
            .id(1)
            .withPropertyKey("topic")
            .spelling(false)
            .please(),
        ],
      })
      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(false)
    })

    it("shows the tracker status instead of Assimilate after a note-level assimilate creates an understanding tracker", async () => {
      let getNoteInfoCallCount = 0
      mockSdkServiceWithImplementation(NoteController, "getNoteInfo", () => {
        getNoteInfoCallCount += 1
        if (getNoteInfoCallCount === 1) {
          return { memoryTrackers: [] }
        }
        return {
          memoryTrackers: [
            makeMe.aMemoryTracker.id(1).spelling(false).please(),
          ],
        }
      })
      assimilateSpy.mockResolvedValue(
        wrapSdkResponse([makeMe.aMemoryTracker.id(1).please()])
      )

      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(false)

      await clickAssimilate(wrapper)

      expect(mockedGoToNextAssimilation).toHaveBeenCalled()
      expect(assimilateButtonEl(wrapper)).toBeNull()
      expect(
        wrapper.element.querySelector(understandingStatusSelector)
      ).not.toBeNull()
    })

    it("shows the tracker status instead of Assimilate when note has an understanding memory tracker", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
      })
      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)).toBeNull()
      expect(
        wrapper.element.querySelector(understandingStatusSelector)
      ).not.toBeNull()
    })
  })
})
