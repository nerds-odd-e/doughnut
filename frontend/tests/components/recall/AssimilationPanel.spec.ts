import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateSpy,
  clickKeepForRecall,
  mountAssimilationPanel,
  mockedIncrementAssimilatedCount,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  note,
  setupAssimilationPanelTests,
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
      const wrapper = mountAssimilationPanel()

      await flushPromises()
      await clickKeepForRecall(wrapper)

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: { noteId: note.id, skipMemoryTracking: false },
      })
      expect(mockedGoToNextAssimilation).toHaveBeenCalled()
      expect(mockedTotalAssimilatedCount.value).toBe(2)
      expect(mockedIncrementAssimilatedCount).toHaveBeenCalledWith(2)
      expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
    })
  })

  describe("NoteInfoBar", () => {
    it("loads note recall info for settings", async () => {
      const wrapper = mountAssimilationPanel()
      await flushPromises()
      expect(
        wrapper.findComponent({ name: "NoteRecallSettingForm" }).exists()
      ).toBe(true)
    })
  })

  describe("SpellingVerificationPopup", () => {
    beforeEach(() => {
      mockSdkService(NoteController, "getNoteInfo", {
        recallSetting: { rememberSpelling: true },
      })
    })

    const getOpaqueContentBlocker = () =>
      document.body.querySelector(
        '[data-test="opaque-content-blocker"]'
      ) as HTMLElement | null

    const closeSpellingVerificationPopup = () => {
      const closeButton = document
        .querySelector('[data-test="spelling-verification-popup"]')
        ?.closest(".modal-mask")
        ?.querySelector(".close-button") as HTMLElement
      closeButton.click()
    }

    it("shows opaque layer to hide note content behind spelling verification", async () => {
      const wrapper = mountAssimilationPanel()
      await flushPromises()

      expect(getOpaqueContentBlocker()).toBeNull()

      await clickKeepForRecall(wrapper)

      const opaqueLayer = getOpaqueContentBlocker()
      expect(opaqueLayer).not.toBeNull()
      expect(opaqueLayer?.style.zIndex).toBe("9989")
      expect(opaqueLayer?.className).toContain("bg-black")
    })

    it("hides opaque layer when spelling popup closes", async () => {
      const wrapper = mountAssimilationPanel()
      await flushPromises()

      await clickKeepForRecall(wrapper)
      expect(getOpaqueContentBlocker()).not.toBeNull()

      closeSpellingVerificationPopup()
      await flushPromises()

      expect(getOpaqueContentBlocker()).toBeNull()
      wrapper.unmount()
    })

    it("closes popup and returns to original state when user closes it", async () => {
      const wrapper = mountAssimilationPanel()
      await flushPromises()

      await clickKeepForRecall(wrapper)
      expect(document.body.textContent).toContain("Verify Spelling")
      expect(assimilateSpy).not.toHaveBeenCalled()

      closeSpellingVerificationPopup()
      await flushPromises()

      expect(document.body.textContent).not.toContain("Verify Spelling")
      expect(assimilateSpy).not.toHaveBeenCalled()
      expect(wrapper.find('[data-test="keep-for-recall"]').exists()).toBe(true)
    })
  })

  describe("keep for repetition when note has memory trackers", () => {
    it("disables keep for repetition when note has memory trackers and no add-spelling-only mode", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        memoryTrackers: [
          { ...makeMe.aMemoryTracker.please(), id: 1, spelling: false },
        ],
      })
      const wrapper = mountAssimilationPanel()
      await flushPromises()

      const keepButton = wrapper.find('[data-test="keep-for-recall"]')
      expect(keepButton.attributes("disabled")).toBeDefined()
    })

    it("enables keep for repetition when remember spelling on and no spelling tracker", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        recallSetting: { rememberSpelling: true },
        memoryTrackers: [
          { ...makeMe.aMemoryTracker.please(), id: 1, spelling: false },
        ],
      })
      const wrapper = mountAssimilationPanel()
      await flushPromises()

      const keepButton = wrapper.find('[data-test="keep-for-recall"]')
      expect(keepButton.attributes("disabled")).toBeUndefined()
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
      const wrapper = mountAssimilationPanel()
      await flushPromises()

      await clickKeepForRecall(wrapper)

      const verifyButton = document.querySelector(
        '[data-test="verify-spelling"]'
      ) as HTMLElement
      verifyButton.click()
      await flushPromises()

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: { noteId: note.id, skipMemoryTracking: false },
      })
      expect(mockedGoToNextAssimilation).toHaveBeenCalled()
      expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
    })
  })
})
