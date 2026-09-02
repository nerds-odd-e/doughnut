import { McqController } from "@generated/donut-backend-api/sdk.gen"
import Mcqs from "@/components/notes/Mcqs.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, vi } from "vitest"
import { createMemoryHistory, createRouter } from "vue-router"

export const exportQuestionGenerationButtonTitle =
  "Export question generation request for ChatGPT"

export const mcqsNote = makeMe.aNote.please()

export const mcqsFixture = [
  makeMe.anMcq
    .withQuestionStem("What is 2+2?")
    .withChoices(["3", "4", "5", "6"])
    .correctAnswerIndex(1)
    .please(),
]

export const sampleMcqExportData = {
  request: {
    model: "gpt-4",
    messages: [],
  },
  title: "Test Note",
} as never

export const mcqsRouter = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: "/", component: { template: "<div />" } }],
})

export let wrapper: VueWrapper

export function setupMcqsTests() {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSdkService(McqController, "list", mcqsFixture)
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })
}

export function mountMcqs(options?: { attachToBody?: boolean }) {
  wrapper = helper
    .component(Mcqs)
    .withProps({ note: mcqsNote })
    .withRouter(mcqsRouter)
    .mount(options?.attachToBody ? { attachTo: document.body } : undefined)
  return wrapper
}

export async function mountMcqsReady(options?: { attachToBody?: boolean }) {
  mountMcqs(options)
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
