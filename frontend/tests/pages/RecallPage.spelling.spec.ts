import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { AnsweredQuestion } from "@generated/donut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { focusDirective } from "@tests/helpers/softKeyboardPrimerTestSupport"
import {
  captureRequestAnimationFrame,
  flushCapturedAnimationFrames,
} from "@tests/components/recall/spellingQuestionDisplayTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import {
  createMemoryTrackerLite,
  createUseRecallDataMock,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

function hasNoteShowLink(wrapper: VueWrapper, noteId: number) {
  const expectedTo = JSON.stringify(noteShowLocation(noteId))
  return wrapper
    .findAll(".router-link")
    .some((link) => link.attributes("to") === expectedTo)
}

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

  const mountAttachedToBody = () =>
    ctx.renderer.currentRoute({ name: "recall" }).mount({
      attachTo: document.body,
      global: { directives: { focus: focusDirective } },
    })

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
    expect(wrapper.find(".daisy-alert-error").text()).toContain(
      "Your answer `test answer` is incorrect."
    )
    expect(hasNoteShowLink(wrapper, 42)).toBe(true)
    expect(
      wrapper
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

    const wrapper = mountAttachedToBody()
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

    expect(wrapper.text()).toContain("Correct!")
    expect(
      hasNoteShowLink(wrapper, previousQuestion.recalledNote.noteTopology.id)
    ).toBe(true)

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

  describe("answer overlapping another note", () => {
    const memoryTrackerId = firstMemoryTrackerId
    let getThresholdExceededSpy: ReturnType<typeof mockSdkService>
    let getRecallPromptSpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
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

      const wrapper = mountAttachedToBody()
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
})
