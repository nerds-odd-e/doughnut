import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import Questions from "@/components/notes/Questions.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, vi } from "vitest"
import { createMemoryHistory, createRouter } from "vue-router"

export const exportQuestionGenerationButtonTitle =
  "Export question generation request for ChatGPT"

export const questionsNote = makeMe.aNote.please()

export const questionsFixture = [
  makeMe.aPredefinedQuestion
    .withQuestionStem("What is 2+2?")
    .withChoices(["3", "4", "5", "6"])
    .correctAnswerIndex(1)
    .please(),
]

export const sampleQuestionExportData = {
  request: {
    model: "gpt-4",
    messages: [],
  },
  title: "Test Note",
} as never

export const questionsRouter = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: "/", component: { template: "<div />" } }],
})

export let wrapper: VueWrapper

export function setupQuestionsTests() {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSdkService(
      PredefinedQuestionController,
      "getAllQuestionByNote",
      questionsFixture
    )
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })
}

export function mountQuestions(options?: { attachToBody?: boolean }) {
  wrapper = helper
    .component(Questions)
    .withProps({ note: questionsNote })
    .withRouter(questionsRouter)
    .mount(options?.attachToBody ? { attachTo: document.body } : undefined)
  return wrapper
}

export async function mountQuestionsReady(options?: {
  attachToBody?: boolean
}) {
  mountQuestions(options)
  await flushPromises()
  return wrapper
}

export function exportQuestionGenerationButton(
  mountedWrapper: VueWrapper = wrapper
) {
  return mountedWrapper.find(
    `button[title="${exportQuestionGenerationButtonTitle}"]`
  )
}

export function exportTextarea() {
  return document.body.querySelector(
    '[data-testid="export-textarea"]'
  ) as HTMLTextAreaElement | null
}

export async function clickExportQuestionGeneration(
  mountedWrapper: VueWrapper = wrapper
) {
  await exportQuestionGenerationButton(mountedWrapper).trigger("click")
  await flushPromises()
}
