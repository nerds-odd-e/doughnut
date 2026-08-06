import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
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

describe("RecallPage spelling quiz", () => {
  const firstMemoryTrackerId = 123
  const ctx = useRecallPageSpecContext({ fakeTimers: true })

  beforeEach(() => {
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
    mockSdkService(
      MemoryTrackerController,
      "askAQuestion",
      makeMe.aRecallQuestion.please()
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(firstMemoryTrackerId, true)],
      })
    )
  })

  it("should handle spelling questions correctly", async () => {
    const note = makeMe.aNote.id(42).please()
    const answerResult: AnsweredQuestion = makeMe.anAnsweredQuestion
      .withNote(note)
      .spelling()
      .withAnswer({ id: 1, correct: false, spellingAnswer: "test answer" })
      .withMemoryTrackerId(123)
      .please()
    const mockedAnswerSpellingCall = mockSdkService(
      RecallPromptController,
      "answerSpelling",
      answerResult
    )
    mockSdkService(MemoryTrackerController, "getThresholdExceeded", {
      thresholdExceeded: false,
    })

    const wrapper = await ctx.mountPage()
    await wrapper.find("input#memory_tracker-answer").setValue("test answer")
    await flushPromises()
    await wrapper.find("form").trigger("submit")
    await flushPromises()
    expect(mockedAnswerSpellingCall).toHaveBeenCalled()

    const answeredSpellingQuestion = wrapper.findComponent({
      name: "AnsweredSpellingQuestion",
    })
    expect(answeredSpellingQuestion.exists()).toBe(true)
    expect(
      answeredSpellingQuestion.find(".daisy-alert-error").text()
    ).toContain("Your answer `test answer` is incorrect.")
    expect(
      answeredSpellingQuestion
        .findComponent({ name: "NoteShow" })
        .props("noteId")
    ).toBe(42)
    expect(
      answeredSpellingQuestion
        .findComponent({ name: "ViewMemoryTrackerLink" })
        .props("memoryTrackerId")
    ).toBe(123)
  })

  it("focuses the spelling answer input when resuming recall", async () => {
    const rafCallbacks = captureRequestAnimationFrame()
    const previousQuestion = makeMe.anAnsweredQuestion
      .withId(1)
      .spelling()
      .withAnswer({ id: 1, correct: true, spellingAnswer: "done" })
      .please()
    ctx.previouslyAnsweredSpy.mockResolvedValueOnce(
      wrapSdkResponse([previousQuestion])
    )
    const recallData = createUseRecallDataMock({
      toRepeat: [createMemoryTrackerLite(firstMemoryTrackerId, true)],
    })
    vi.mocked(useRecallData).mockReturnValue(recallData)

    const wrapper = ctx.renderer.currentRoute({ name: "recall" }).mount({
      attachTo: document.body,
      global: { directives: { focus: focusDirective } },
    })
    await flushPromises()
    await nextTick()
    flushCapturedAnimationFrames(rafCallbacks)
    await flushPromises()

    const pauseButton = wrapper.find(
      'button[title="view last answered question"]'
    )
    expect(pauseButton.exists()).toBe(true)
    await pauseButton.trigger("click")
    await flushPromises()

    const spellingInput = document.querySelector(
      "input#memory_tracker-answer"
    ) as HTMLInputElement
    expect(spellingInput).toBeTruthy()
    spellingInput.blur()
    expect(document.activeElement).not.toBe(spellingInput)

    recallData.shouldResumeRecall.value = true
    await flushPromises()
    await nextTick()
    flushCapturedAnimationFrames(rafCallbacks)
    await flushPromises()

    expect(document.activeElement).toBe(spellingInput)
    wrapper.unmount()
  })
})
