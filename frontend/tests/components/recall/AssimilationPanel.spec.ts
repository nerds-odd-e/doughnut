import {
  AssimilationSequenceSkipController,
  MemoryTrackerController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
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
  clickRemoveFromRecallAndConfirm,
  clickReturnToSequence,
  clickSkipAndConfirm,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  mountAssimilationPanelReady,
  note,
  removeFromRecallButtonEl,
  returnToSequenceButtonEl,
  reviveButtonSelector,
  setupAssimilationPanelTests,
  skipButtonEl,
  skipSequenceSpy,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel", () => {
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

  it("shows Return to sequence instead of Skip or Revive when the note is sequence-skipped", async () => {
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo.skippedPropertyKeys([""]).please()
    )
    const wrapper = await mountAssimilationPanelReady()

    expect(returnToSequenceButtonEl(wrapper)).not.toBeNull()
    expect(skipButtonEl(wrapper)).toBeNull()
    expect(wrapper.find(reviveButtonSelector).exists()).toBe(false)
  })

  it("returns the note to the sequence without creating a tracker or reviving", async () => {
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

  it("removes the understanding tracker from recall and shows Revive", async () => {
    let removedFromTracking = false
    mockSdkServiceWithImplementation(NoteController, "getNoteInfo", () =>
      makeMe.aNoteRecallInfo
        .memoryTrackers([
          makeMe.aMemoryTracker
            .id(7)
            .spelling(false)
            .removedFromTracking(removedFromTracking)
            .please(),
        ])
        .please()
    )
    const removeSpy = mockSdkService(
      MemoryTrackerController,
      "removeFromRepeating",
      makeMe.aMemoryTracker.id(7).removedFromTracking(true).please()
    )
    removeSpy.mockImplementation(async () => {
      removedFromTracking = true
      return wrapSdkResponse(
        makeMe.aMemoryTracker.id(7).removedFromTracking(true).please()
      )
    })
    const wrapper = await mountAssimilationPanelReady()

    await clickRemoveFromRecallAndConfirm(wrapper)

    expect(removeSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 7 },
    })
    expect(skipSequenceSpy).not.toHaveBeenCalled()
    expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
    expect(wrapper.find(reviveButtonSelector).exists()).toBe(true)
    expect(skipButtonEl(wrapper)).toBeNull()
    expect(removeFromRecallButtonEl(wrapper)).toBeNull()
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

    it("disables assimilate after note-level assimilate when next unit stays on the same note", async () => {
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
      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(true)
    })

    it("disables assimilate when note has memory trackers", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
      })
      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(true)
    })
  })
})
