import {
  MemoryTrackerController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateSpy,
  mountAssimilationPanel,
  mockedRequestDueRecallsRefresh,
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

const reviveButtonSelector = '[data-test="revive"]'

describe("AssimilationPanel revive", () => {
  it("calls reEnable for all skipped note-level trackers and reloads note info", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [
        {
          ...makeMe.aMemoryTracker.please(),
          id: 10,
          spelling: false,
          removedFromTracking: true,
        },
        {
          ...makeMe.aMemoryTracker.please(),
          id: 11,
          spelling: true,
          removedFromTracking: true,
        },
      ],
    })
    const reEnableSpy = mockSdkService(MemoryTrackerController, "reEnable", {
      ...makeMe.aMemoryTracker.please(),
      removedFromTracking: false,
    })
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

  it("shows Skip recall when note-level trackers are active", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [
        {
          ...makeMe.aMemoryTracker.please(),
          id: 1,
          spelling: false,
          removedFromTracking: false,
        },
      ],
    })
    const wrapper = mountAssimilationPanel()
    await flushPromises()

    expect(wrapper.find(reviveButtonSelector).exists()).toBe(false)
    expect(wrapper.find('[value="Skip recall"]').exists()).toBe(true)
    wrapper.unmount()
  })
})
