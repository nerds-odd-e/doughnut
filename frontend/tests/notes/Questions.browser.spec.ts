import { describe, it, vi, expect, beforeEach, afterEach } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import Questions from "@/components/notes/Questions.vue"
import { reactive } from "vue"

const mockRoute = reactive({ name: "", path: "", params: {}, query: {} })
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => mockRoute,
    useRouter: () => ({
      push: vi.fn(),
    }),
  }
})

describe("Questions", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>
  const note = makeMe.aNote.please()
  const questions = [
    makeMe.aPredefinedQuestion
      .withQuestionStem("What is 2+2?")
      .withChoices(["3", "4", "5", "6"])
      .correctAnswerIndex(1)
      .please(),
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    mockSdkService("getAllQuestionByNote", questions)
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("renders questions table when questions exist", async () => {
    wrapper = helper
      .component(Questions)
      .withProps({ note })
      .mount({ attachTo: document.body })

    await vi.waitUntil(() => wrapper.text().includes("What is 2+2?"))
    expect(wrapper.text()).toContain("What is 2+2?")
  })

  it("shows export dialog when export button is clicked", async () => {
    const exportData = {
      request: {
        model: "gpt-4",
        messages: [],
      },
      title: "Test Note",
    } as never
    const exportQuestionGenerationSpy = mockSdkService(
      "exportQuestionGeneration",
      exportData
    )

    wrapper = helper
      .component(Questions)
      .withProps({ note })
      .mount({ attachTo: document.body })

    await flushPromises()

    const exportButton = wrapper.find(
      '[aria-label="Export question generation request"]'
    )
    await exportButton.trigger("click")

    await vi.waitUntil(() =>
      document.body.querySelector('[data-testid="export-textarea"]')
    )
    expect(
      document.body.querySelector('[data-testid="export-textarea"]')
    ).toBeTruthy()

    expect(exportQuestionGenerationSpy).toHaveBeenCalledWith({
      path: { note: note.id },
    })
  })
})
