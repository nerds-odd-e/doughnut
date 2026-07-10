import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, expect, it, vi, beforeEach } from "vitest"
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
  clickAssimilate,
  clickVerifySpelling,
  closeSpellingVerificationPopup,
  mountAssimilationPanelReady,
  mockedIncrementAssimilatedCount,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  note,
  opaqueContentBlockerEl,
  setupAssimilationPanelTests,
  setupRememberSpellingRecall,
  spellingVerificationPopupEl,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useAssimilationCount")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel", () => {
  describe("normal assimilation", () => {
    it("calls goToNextAssimilation and increments counts correctly when assimilating normally", async () => {
      assimilateSpy.mockResolvedValue(
        wrapSdkResponse([
          { id: 1, removedFromTracking: false },
          { id: 2, removedFromTracking: true },
          { id: 3, removedFromTracking: false },
        ])
      )
      const wrapper = await mountAssimilationPanelReady()

      await clickAssimilate(wrapper)

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: { noteId: note.id },
      })
      expect(mockedGoToNextAssimilation).toHaveBeenCalled()
      expect(mockedTotalAssimilatedCount.value).toBe(2)
      expect(mockedIncrementAssimilatedCount).toHaveBeenCalledWith(2)
      expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
    })
  })

  describe("NoteInfoBar", () => {
    it("loads note recall info for settings", async () => {
      const wrapper = await mountAssimilationPanelReady()
      expect(
        wrapper.findComponent({ name: "NoteRecallSettingForm" }).exists()
      ).toBe(true)
    })
  })

  describe("SpellingVerificationPopup", () => {
    beforeEach(() => {
      setupRememberSpellingRecall()
    })

    it("shows opaque layer to hide note content behind spelling verification", async () => {
      const wrapper = await mountAssimilationPanelReady()

      expect(opaqueContentBlockerEl()).toBeNull()

      await clickAssimilate(wrapper)

      const opaqueLayer = opaqueContentBlockerEl()
      expect(opaqueLayer).not.toBeNull()
      expect(opaqueLayer?.style.zIndex).toBe("9989")
      expect(opaqueLayer?.className).toContain("bg-black")
    })

    it("closes spelling verification and restores assimilate panel without assimilating", async () => {
      const wrapper = await mountAssimilationPanelReady()

      await clickAssimilate(wrapper)
      expect(opaqueContentBlockerEl()).not.toBeNull()
      expect(spellingVerificationPopupEl()).not.toBeNull()
      expect(assimilateSpy).not.toHaveBeenCalled()

      await closeSpellingVerificationPopup()

      expect(opaqueContentBlockerEl()).toBeNull()
      expect(spellingVerificationPopupEl()).toBeNull()
      expect(assimilateSpy).not.toHaveBeenCalled()
      expect(assimilateButtonEl(wrapper)).not.toBeNull()
    })
  })

  describe("assimilate when note has memory trackers", () => {
    it("enables assimilate when note has only a property memory tracker", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        memoryTrackers: [
          {
            ...makeMe.aMemoryTracker.please(),
            id: 1,
            propertyKey: "topic",
            spelling: false,
          },
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
            { ...makeMe.aMemoryTracker.please(), id: 1, spelling: false },
          ],
        }
      })
      assimilateSpy.mockResolvedValue(
        wrapSdkResponse([{ id: 1, removedFromTracking: false }])
      )

      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(false)

      await clickAssimilate(wrapper)

      expect(mockedGoToNextAssimilation).toHaveBeenCalled()
      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(true)
    })

    it("disables assimilate when note has memory trackers and no add-spelling-only mode", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        memoryTrackers: [
          { ...makeMe.aMemoryTracker.please(), id: 1, spelling: false },
        ],
      })
      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(true)
    })

    it("enables assimilate when remember spelling on and no spelling tracker", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        recallSetting: { rememberSpelling: true },
        memoryTrackers: [
          { ...makeMe.aMemoryTracker.please(), id: 1, spelling: false },
        ],
      })
      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(false)
    })

    it("adds only spelling memory tracker when in add-spelling-only mode", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        recallSetting: { rememberSpelling: true },
        memoryTrackers: [
          { ...makeMe.aMemoryTracker.please(), id: 1, spelling: false },
        ],
      })
      mockSdkService(NoteController, "verifySpelling", { correct: true })
      assimilateSpy.mockResolvedValue(
        wrapSdkResponse([
          {
            ...makeMe.aMemoryTracker.please(),
            id: 2,
            spelling: true,
            removedFromTracking: false,
          },
        ])
      )
      const wrapper = await mountAssimilationPanelReady()

      await clickAssimilate(wrapper)

      await clickVerifySpelling()

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: { noteId: note.id },
      })
      expect(mockedGoToNextAssimilation).toHaveBeenCalled()
      expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
    })
  })
})
