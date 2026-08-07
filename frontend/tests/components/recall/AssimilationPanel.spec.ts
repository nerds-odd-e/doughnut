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
  assimilatedCountOfTheDay,
  clickAssimilate,
  clickAssimilateAsCommissioned,
  clickVerifySpelling,
  closeSpellingVerificationPopup,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  mountAssimilationPanelReady,
  note,
  opaqueContentBlockerEl,
  setupAssimilationPanelTests,
  setupRememberSpellingRecall,
  spellingVerificationPopupEl,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel", () => {
  describe("normal assimilation", () => {
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

    it("posts assimilateAsCommissioned and stays on note without navigating", async () => {
      assimilateSpy.mockResolvedValue(
        wrapSdkResponse([makeMe.aMemoryTracker.id(1).commissioned().please()])
      )
      const wrapper = await mountAssimilationPanelReady()

      await clickAssimilateAsCommissioned(wrapper)

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: { noteId: note.id, assimilateAsCommissioned: true },
      })
      expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
      expect(mockedTotalAssimilatedCount.value).toBe(0)
      expect(assimilatedCountOfTheDay.value).toBe(0)
      expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
      expect(spellingVerificationPopupEl()).toBeNull()
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

    it("disables assimilate when note has memory trackers and no add-spelling-only mode", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
      })
      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(true)
    })

    it("enables assimilate when remember spelling on and no spelling tracker", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        recallSetting: { rememberSpelling: true },
        memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
      })
      const wrapper = await mountAssimilationPanelReady()

      expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(false)
    })

    it("adds only spelling memory tracker when in add-spelling-only mode", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        recallSetting: { rememberSpelling: true },
        memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
      })
      mockSdkService(NoteController, "verifySpelling", { correct: true })
      assimilateSpy.mockResolvedValue(
        wrapSdkResponse([makeMe.aMemoryTracker.id(2).spelling(true).please()])
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
