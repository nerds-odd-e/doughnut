import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { AnsweredQuestion } from "@generated/donut-backend-api"
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

describe("overlap try-again stay and retry", () => {
  const memoryTrackerId = 123
  const ctx = useRecallPageSpecContext({ fakeTimers: true })
  let getThresholdExceededSpy: ReturnType<typeof mockSdkService>
  let getRecallPromptSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
    getRecallPromptSpy = mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      makeMe.aRecallPrompt.withSpellingStem("Spell").please()
    )
    getThresholdExceededSpy = mockSdkService(
      MemoryTrackerController,
      "getThresholdExceeded",
      { thresholdExceeded: false }
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [
          createMemoryTrackerLite(memoryTrackerId, true),
          createMemoryTrackerLite(456, true),
        ],
      })
    )
  })

  it("stays on the same tracker, skips threshold, and remounts spelling on Try again", async () => {
    const overlapResult: AnsweredQuestion = makeMe.anAnsweredQuestion
      .overlap("Shared Title")
      .withMemoryTrackerId(memoryTrackerId)
      .please()

    const wrapper = await ctx.mountPage()

    type ExposedVM = { currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    wrapper
      .findComponent({ name: "RecallPromptCard" })
      .vm.$emit("answered", overlapResult)
    await flushPromises()

    expect(vm.currentIndex).toBe(0)
    expect(getThresholdExceededSpy).not.toHaveBeenCalled()

    const getRecallPromptCallsBeforeRetry = getRecallPromptSpy.mock.calls.length
    await wrapper.find('[data-testid="overlap-try-again"]').trigger("click")
    await flushPromises()

    expect(
      wrapper.findComponent({ name: "AnsweredSpellingQuestion" }).exists()
    ).toBe(false)
    expect(vm.currentIndex).toBe(0)
    expect(
      wrapper
        .findComponent({ name: "RecallPromptCard" })
        .props("spellingRetryNonce")
    ).toBe(1)
    expect(getRecallPromptSpy.mock.calls.length).toBeGreaterThan(
      getRecallPromptCallsBeforeRetry
    )
  })
})
