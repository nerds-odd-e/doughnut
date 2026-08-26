import {
  MemoryTrackerController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateSpy,
  mountAssimilationPanel,
  mockedRequestDueRecallsRefresh,
  removeFromRecallButtonSelector,
  reviveButtonSelector,
  setupAssimilationPanelTests,
  skipButtonSelector,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel revive", () => {
  it("calls reEnable for all skipped note-level trackers and reloads note info", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [
        makeMe.aMemoryTracker
          .id(10)
          .spelling(false)
          .removedFromTracking(true)
          .please(),
        makeMe.aMemoryTracker
          .id(11)
          .spelling(true)
          .removedFromTracking(true)
          .please(),
      ],
    })
    const reEnableSpy = mockSdkService(
      MemoryTrackerController,
      "reEnable",
      makeMe.aMemoryTracker.removedFromTracking(false).please()
    )
    const wrapper = mountAssimilationPanel()
    await flushPromises()

    const reviveButton = wrapper.find(reviveButtonSelector)
    expect(reviveButton.exists()).toBe(true)
    await reviveButton.trigger("click")
    await flushPromises()

    expect(reEnableSpy).toHaveBeenCalledTimes(2)
    expect(reEnableSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 10 },
    })
    expect(reEnableSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 11 },
    })
    expect(assimilateSpy).not.toHaveBeenCalled()
    expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
    expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
    wrapper.unmount()
  })

  it("shows Remove from recall instead of Skip when note-level trackers are active", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
    })
    const wrapper = mountAssimilationPanel()
    await flushPromises()

    expect(wrapper.find(reviveButtonSelector).exists()).toBe(false)
    expect(wrapper.find(skipButtonSelector).exists()).toBe(false)
    expect(wrapper.find(removeFromRecallButtonSelector).exists()).toBe(true)
    wrapper.unmount()
  })
})
