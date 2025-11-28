import MemoryTrackerPage from "@/pages/MemoryTrackerPage.vue"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

describe("MemoryTrackerPage", () => {
  const memoryTrackerId = 123
  const answeredQuestion = makeMe.anAnsweredQuestion.please()

  beforeEach(() => {
    mockSdkService("getLastAnsweredQuestion", answeredQuestion)
  })

  it("fetches and displays last answered question", async () => {
    const getLastAnsweredQuestionSpy = mockSdkService(
      "getLastAnsweredQuestion",
      answeredQuestion
    )
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(getLastAnsweredQuestionSpy).toHaveBeenCalledWith({
      path: { memoryTracker: memoryTrackerId },
    })

    const answeredQuestionComponent = wrapper.findComponent({
      name: "AnsweredQuestionComponent",
    })
    expect(answeredQuestionComponent.exists()).toBe(true)
    expect(answeredQuestionComponent.props("answeredQuestion")).toEqual(
      answeredQuestion
    )
    expect(answeredQuestionComponent.props("conversationButton")).toBe(true)
  })

  it("shows loading state while fetching", async () => {
    mockSdkService("getLastAnsweredQuestion", answeredQuestion)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    const contentLoader = wrapper.findComponent({ name: "ContentLoader" })
    expect(contentLoader.exists()).toBe(true)

    await flushPromises()

    const contentLoaderAfter = wrapper.findComponent({ name: "ContentLoader" })
    expect(contentLoaderAfter.exists()).toBe(false)
  })

  it("shows message when no answered question exists", async () => {
    mockSdkService("getLastAnsweredQuestion", null)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("No answered question found")
    const answeredQuestionComponent = wrapper.findComponent({
      name: "AnsweredQuestionComponent",
    })
    expect(answeredQuestionComponent.exists()).toBe(false)
  })

  it("shows error message when API call fails", async () => {
    vi.spyOn(
      MemoryTrackerController,
      "getLastAnsweredQuestion"
    ).mockResolvedValue(wrapSdkError("Error"))
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Error loading answered question")
  })
})
