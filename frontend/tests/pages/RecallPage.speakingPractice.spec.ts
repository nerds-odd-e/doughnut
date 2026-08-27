import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  createMemoryTrackerLite,
  createUseRecallDataMock,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/components/commons/Popups/usePopups")

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/", fullPath: "/" }),
    useRouter: () => ({ currentRoute: { value: { name: "recall" } } }),
  }
})

const speakingPracticeInputSelector = '[data-testid="speaking-practice-input"]'

describe("RecallPage speaking practice input", () => {
  const memoryTrackerId = 123
  const ctx = useRecallPageSpecContext()

  beforeEach(() => {
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
    mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      makeMe.aRecallPrompt.please()
    )
    mockSdkService(MemoryTrackerController, "getThresholdExceeded", {
      thresholdExceeded: false,
    })
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(memoryTrackerId)],
      })
    )
  })

  it("shows speaking practice input under answered MCQ, not during quiz", async () => {
    const wrapper = await ctx.mountPage()
    expect(wrapper.find(speakingPracticeInputSelector).exists()).toBe(false)

    const wrongAnswer = makeMe.anAnsweredQuestion
      .withNote(makeMe.aNote.please())
      .withMcq(makeMe.anMcq.please())
      .withAnswer({ id: 1, correct: false, choiceIndex: 1 })
      .withMemoryTrackerId(memoryTrackerId)
      .please()
    wrapper.findComponent({ name: "Quiz" }).vm.$emit("answered", wrongAnswer)
    await flushPromises()

    expect(wrapper.find(speakingPracticeInputSelector).exists()).toBe(true)
  })
})
