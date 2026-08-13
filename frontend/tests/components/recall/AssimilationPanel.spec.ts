import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
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
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  mountAssimilationPanelReady,
  note,
  setupAssimilationPanelTests,
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
      wrapSdkResponse([
        makeMe.aMemoryTracker.id(1).please(),
        makeMe.aMemoryTracker.id(2).removedFromTracking(true).please(),
        makeMe.aMemoryTracker.id(3).please(),
      ])
    )
    const wrapper = await mountAssimilationPanelReady()

    await clickAssimilate(wrapper)

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: { noteId: note.id },
    })
    expect(mockedGoToNextAssimilation).toHaveBeenCalled()
    expect(mockedTotalAssimilatedCount.value).toBe(2)
    expect(assimilatedCountOfTheDay.value).toBe(2)
    expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
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
