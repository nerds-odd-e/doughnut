import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, expect, it, vi } from "vitest"
import {
  memoryTrackerId,
  mockMemoryTrackerPageApis,
  mockShowMemoryTrackerSequence,
  mountMemoryTrackerPage,
  mountMemoryTrackerPageReady,
} from "./memoryTrackerPageTestSupport"
import {
  removeFromRecallButtonTitle,
  reviveButtonTitle,
  skippedBannerText,
  skippedMemoryTracker,
} from "./memoryTrackerPageViewTestSupport"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
    }),
  }
})

describe("MemoryTrackerPage", () => {
  it("fetches memory tracker data on mount", async () => {
    const { getRecallHistorySpy, showMemoryTrackerSpy } =
      mockMemoryTrackerPageApis({
        recallHistory: [
          makeMe.aRecallHistoryItem
            .recallPrompt(
              makeMe.aRecallPromptHistoryItem
                .withQuestionStem("Loaded question")
                .please()
            )
            .please(),
        ],
      })
    const wrapper = mountMemoryTrackerPage()
    await flushPromises()

    expect(getRecallHistorySpy).toHaveBeenCalledWith({
      path: { memoryTracker: memoryTrackerId },
    })
    expect(showMemoryTrackerSpy).toHaveBeenCalledWith({
      path: { memoryTracker: memoryTrackerId },
    })
    expect(wrapper.text()).toContain("Loaded question")
  })

  it("shows loading spinner while fetching", async () => {
    mockMemoryTrackerPageApis()
    const wrapper = mountMemoryTrackerPage()

    expect(wrapper.find(".daisy-loading-spinner[data-app-busy]").exists()).toBe(
      true
    )

    await flushPromises()

    expect(wrapper.find(".daisy-loading-spinner[data-app-busy]").exists()).toBe(
      false
    )
  })

  it("shows message when there is no recall history", async () => {
    const wrapper = await mountMemoryTrackerPageReady({
      recallHistory: [],
    })

    expect(wrapper.text()).toContain("No recall history found")
  })

  it("shows error message when API call fails", async () => {
    vi.spyOn(MemoryTrackerController, "getRecallHistory").mockResolvedValue(
      wrapSdkError("Error")
    )
    vi.spyOn(MemoryTrackerController, "showMemoryTracker").mockResolvedValue(
      wrapSdkError("Error")
    )
    const wrapper = mountMemoryTrackerPage()
    await flushPromises()

    expect(wrapper.text()).toContain("Error loading memory tracker data")
  })

  it("refetches tracker after revive and leaves skipped state", async () => {
    const skippedTracker = skippedMemoryTracker()
    const activeTracker = makeMe.aMemoryTracker
      .removedFromTracking(false)
      .please()

    mockMemoryTrackerPageApis({
      recallHistory: [
        makeMe.aRecallHistoryItem
          .recallPrompt(makeMe.aRecallPromptHistoryItem.please())
          .please(),
      ],
      memoryTracker: skippedTracker,
    })
    mockShowMemoryTrackerSequence(skippedTracker, activeTracker)
    mockSdkService(MemoryTrackerController, "reEnable", activeTracker)

    const wrapper = mountMemoryTrackerPage()
    await flushPromises()

    expect(wrapper.text()).toContain(skippedBannerText)

    await wrapper.find(`button[title="${reviveButtonTitle}"]`).trigger("click")
    await flushPromises()

    expect(wrapper.text()).not.toContain(skippedBannerText)
    expect(
      wrapper.find(`button[title="${removeFromRecallButtonTitle}"]`).exists()
    ).toBe(true)
  })
})
