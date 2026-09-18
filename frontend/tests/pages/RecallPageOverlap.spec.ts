import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { AnsweredQuestion } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { focusDirective } from "@tests/helpers/softKeyboardPrimerTestSupport"
import {
  captureRequestAnimationFrame,
  flushCapturedAnimationFrames,
} from "@tests/components/recall/spellingQuestionDisplayTestSupport"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
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

describe("overlap try-again stays on the active spelling question", () => {
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

  it("keeps the current tracker, shows the overlap explanation, and refocuses an emptied input", async () => {
    const overlapResult: AnsweredQuestion = makeMe.anAnsweredQuestion
      .overlap("Shared Title")
      .withMemoryTrackerId(memoryTrackerId)
      .please()
    const answerSpellingSpy = mockSdkService(
      RecallPromptController,
      "answerSpelling",
      overlapResult
    )
    const rafCallbacks = captureRequestAnimationFrame()

    const wrapper = ctx.renderer.currentRoute({ name: "recall" }).mount({
      attachTo: document.body,
      global: { directives: { focus: focusDirective } },
    })
    await flushPromises()
    await nextTick()
    flushCapturedAnimationFrames(rafCallbacks)
    await flushPromises()

    type ExposedVM = { currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    const getRecallPromptCallsBeforeAnswer =
      getRecallPromptSpy.mock.calls.length

    await wrapper.find("input#memory_tracker-answer").setValue("Shared Title")
    await wrapper.find("form").trigger("submit")
    await flushPromises()
    await nextTick()
    flushCapturedAnimationFrames(rafCallbacks)
    await flushPromises()

    expect(answerSpellingSpy).toHaveBeenCalled()
    expect(getThresholdExceededSpy).not.toHaveBeenCalled()
    expect(vm.currentIndex).toBe(0)
    expect(getRecallPromptSpy.mock.calls.length).toBeGreaterThan(
      getRecallPromptCallsBeforeAnswer
    )

    expect(
      wrapper.find('[data-testid="spelling-overlap-feedback"]').text()
    ).toContain(
      "Your answer matches an overlapped note, but that's different from the expected answer"
    )

    const spellingInput = document.querySelector(
      "input#memory_tracker-answer"
    ) as HTMLInputElement
    expect(spellingInput.value).toBe("")
    expect(document.activeElement).toBe(spellingInput)
  })
})
