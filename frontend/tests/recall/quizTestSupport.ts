import {
  MemoryTrackerController,
  NoteController,
  RecallPromptController,
} from "@generated/doughnut-backend-api/sdk.gen"
import Quiz from "@/components/recall/Quiz.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  wrapSdkResponse,
  wrapSdkError,
} from "@tests/helpers"
import type {
  AnsweredQuestion,
  MemoryTrackerLite,
  RecallQuestion,
} from "@generated/doughnut-backend-api"
import { afterEach, beforeEach, vi } from "vitest"
import { spellingAnswerInputSelector } from "@tests/components/recall/spellingQuestionDisplayTestSupport"
import { createDeferredGate } from "@tests/components/recall/noteRefinementTestSupport"

export { createDeferredGate, wrapSdkError, wrapSdkResponse }

export const contentLoaderSelector = ".daisy-loading.daisy-loading-spinner"
export const contestableDummyInputSelector =
  '[data-testid="contestable-dummy-input"]'
export const recallPromptSelector = ".recall-prompt"
export const justReviewButtonText = "Yes, I remember"

export let askAQuestionSpy: ReturnType<typeof mockSdkService>
export let wrapper: VueWrapper

let recallPrompt: RecallQuestion

export function getRecallPrompt() {
  return recallPrompt
}

export function setupQuizTests() {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    recallPrompt = makeMe.aRecallQuestion.please()
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
    askAQuestionSpy = mockSdkService(
      MemoryTrackerController,
      "askAQuestion",
      recallPrompt
    )
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.useRealTimers()
  })
}

export function createMemoryTrackerLite(
  id: number,
  spelling = false
): MemoryTrackerLite {
  return { memoryTrackerId: id, spelling }
}

export function mountQuiz(
  memoryTrackerIds: number[],
  eagerFetchCount: number,
  spelling = false
) {
  const memoryTrackers = memoryTrackerIds.map((id) =>
    createMemoryTrackerLite(id, spelling)
  )
  wrapper = helper
    .component(Quiz)
    .withRouter()
    .withCleanStorage()
    .withProps({
      memoryTrackers,
      currentIndex: 0,
      eagerFetchCount,
    })
    .mount({ attachTo: document.body })
  return wrapper
}

export async function mountQuizReady(
  memoryTrackerIds: number[],
  eagerFetchCount: number,
  spelling = false
) {
  const mounted = mountQuiz(memoryTrackerIds, eagerFetchCount, spelling)
  await flushPromises()
  return mounted
}

export function contentLoaderVisible(quizWrapper: VueWrapper) {
  return quizWrapper.find(contentLoaderSelector).exists()
}

export function justReviewVisible(quizWrapper: VueWrapper) {
  return quizWrapper.text().includes(justReviewButtonText)
}

export function spellingQuestionVisible(quizWrapper: VueWrapper) {
  return quizWrapper.find(spellingAnswerInputSelector).exists()
}

export function contestableQuestionVisible(quizWrapper: VueWrapper) {
  return quizWrapper.find(recallPromptSelector).exists()
}

export function contestableDummyInput(quizWrapper: VueWrapper) {
  return quizWrapper.find<HTMLTextAreaElement>(contestableDummyInputSelector)
}

export async function submitSpellingAnswerFromQuiz(
  quizWrapper: VueWrapper,
  answer = "cat"
) {
  await quizWrapper.find(spellingAnswerInputSelector).setValue(answer)
  await quizWrapper.find("form").trigger("submit")
  await flushPromises()
}

export function mockSpellingRecallServices(stem = "Spell the word 'cat'") {
  const spellingRecallPrompt = makeMe.aRecallQuestion
    .withSpellingStem(stem)
    .please()
  askAQuestionSpy.mockResolvedValue(wrapSdkResponse(spellingRecallPrompt))

  const memoryTracker = makeMe.aMemoryTracker.please()
  if (memoryTracker.note) {
    // @ts-expect-error - clozeDescription is a method on Note, not a property
    memoryTracker.note.clozeDescription = {
      clozeDetails: () => `<p>${stem}</p>\n`,
    }
  }
  mockSdkService(MemoryTrackerController, "showMemoryTracker", memoryTracker)
  return spellingRecallPrompt
}

export function mockAnswerSpelling(answerResult: AnsweredQuestion) {
  return mockSdkService(RecallPromptController, "answerSpelling", answerResult)
}
