import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { beforeEach, describe, expect, it } from "vitest"
import {
  defaultMemoryTrackerId,
  mockMemoryTrackerPageViewDefaults,
  mountMemoryTrackerPageViewReady,
  removeFromRecallButton,
  reviveButton,
  skippedBannerText,
  skippedMemoryTracker,
} from "./memoryTrackerPageViewTestSupport"

describe("MemoryTrackerPageView skipped tracker", () => {
  beforeEach(() => {
    mockMemoryTrackerPageViewDefaults()
  })

  it("shows skipped banner, revive button, and hides remove-from-recall", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Test question")
          .please(),
      ],
      memoryTracker: skippedMemoryTracker(),
    })

    expect(wrapper.text()).toContain(skippedBannerText)
    expect(reviveButton(wrapper).exists()).toBe(true)
    expect(removeFromRecallButton(wrapper).exists()).toBe(false)
  })

  it("still shows recall prompts when skipped", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Test question")
          .withChoices(["A", "B", "C"])
          .please(),
      ],
      memoryTracker: skippedMemoryTracker(),
    })

    expect(wrapper.text()).toContain("Test question")
    expect(wrapper.text()).toContain("A")
  })

  it("calls re-enable and emits refresh when revive is clicked", async () => {
    const memoryTracker = skippedMemoryTracker()
    const reEnableSpy = mockSdkService(MemoryTrackerController, "reEnable", {
      ...memoryTracker,
      removedFromTracking: false,
    })

    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Test question")
          .please(),
      ],
      memoryTracker,
    })

    await reviveButton(wrapper).trigger("click")
    await flushPromises()

    expect(reEnableSpy).toHaveBeenCalledWith({
      path: { memoryTracker: defaultMemoryTrackerId },
    })
    expect(wrapper.emitted("refresh")).toHaveLength(1)
  })
})
