import {
  MemoryTrackerController,
  NoteController,
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
import { beforeEach, describe, expect, it } from "vitest"
import { nextTick } from "vue"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

const memoryTrackerId = 123
const ctx = useRecallPageSpecContext({ fakeTimers: true })
let getRecallPromptSpy: ReturnType<typeof mockSdkService>
let getThresholdExceededSpy: ReturnType<typeof mockSdkService>

beforeEach(() => {
  mockSdkService(
    MemoryTrackerController,
    "showMemoryTracker",
    makeMe.aMemoryTracker.please()
  )
  getRecallPromptSpy = mockSdkService(
    MemoryTrackerController,
    "getRecallPrompt",
    makeMe.aRecallPrompt.please()
  )
  getThresholdExceededSpy = mockSdkService(
    MemoryTrackerController,
    "getThresholdExceeded",
    { thresholdExceeded: false }
  )
  givenRecallQueue(createMemoryTrackerLite(memoryTrackerId, true))
})

function hasNoteShowLink(wrapper: VueWrapper, noteId: number) {
  const expectedTo = JSON.stringify(noteShowLocation(noteId))
  return wrapper
    .findAll(".router-link")
    .some((link) => link.attributes("to") === expectedTo)
}

describe("RecallPage spelling quiz", () => {
  const mountAttachedToBody = () =>
    ctx.mountPage({
      attachTo: document.body,
      global: { directives: { focus: focusDirective } },
    })

  it("shows incorrect feedback with the reviewed note's context after a wrong answer", async () => {
    const noteTitle = "Sedition"
    const notebookName = "Political History"
    const noteRealm = makeMe.aNoteRealm
      .title(noteTitle)
      .notebookName(notebookName)
      .content(`---
register: formal
---

Inciting rebellion against authority.`)
      .please()
    makeMe.aNoteRealm.title("Mutiny").under(noteRealm).please()
    mockSdkService(NoteController, "showNote", noteRealm)
    mockSdkService(
      RecallPromptController,
      "answerSpelling",
      makeMe.anAnsweredQuestion
        .withNote(noteRealm.note)
        .spelling()
        .withAnswer({ id: 1, correct: false, spellingAnswer: "sedation" })
        .withMemoryTrackerId(memoryTrackerId)
        .please()
    )

    const wrapper = await ctx.mountPage()
    await wrapper.find("input#memory_tracker-answer").setValue("sedation")
    await wrapper.find("form").trigger("submit")
    await flushPromises()

    expect(wrapper.find(".daisy-alert-error").text()).toContain(
      "Your answer `sedation` is incorrect."
    )
    expect(hasNoteShowLink(wrapper, noteRealm.id)).toBe(true)
    expect(
      wrapper
        .findComponent({ name: "ViewMemoryTrackerLink" })
        .props("memoryTrackerId")
    ).toBe(memoryTrackerId)

    const resultText = wrapper.text()
    expect(resultText.split(noteTitle).length - 1).toBe(1)
    expect(resultText.split(notebookName).length - 1).toBe(1)

    const noteUnderQuestion = wrapper.find(".note-under-question")
    const noteContext = noteUnderQuestion.find('[aria-label="Note context"]')
    expect(noteContext.exists()).toBe(true)
    const noteContextText = noteContext.text()
    for (const expected of [
      "register",
      "formal",
      "Inciting rebellion against authority.",
      "Mutiny",
    ]) {
      expect(noteContextText).toContain(expected)
    }
  })

  it("focuses the spelling answer input when resuming recall", async () => {
    const rafCallbacks = captureRequestAnimationFrame()
    const noteRealm = makeMe.aNoteRealm.please()
    mockSdkService(NoteController, "showNote", noteRealm)
    const previousQuestion = makeMe.anAnsweredQuestion
      .withId(1)
      .withNote(noteRealm.note)
      .spelling()
      .withAnswer({ id: 1, correct: true, spellingAnswer: "done" })
      .please()
    ctx.previouslyAnsweredSpy.mockResolvedValueOnce(
      wrapSdkResponse([previousQuestion])
    )
    const wrapper = await mountAttachedToBody()
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

    useRecallData().shouldResumeRecall.value = true
    await flushPromises()
    await nextTick()
    flushCapturedAnimationFrames(rafCallbacks)
    await flushPromises()

    expect(document.activeElement).toBe(spellingInput)
  })

  describe("answer overlapping another note", () => {
    beforeEach(() => {
      givenRecallQueue(
        createMemoryTrackerLite(memoryTrackerId, true),
        createMemoryTrackerLite(456, true)
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

      const wrapper = await mountAttachedToBody()
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
