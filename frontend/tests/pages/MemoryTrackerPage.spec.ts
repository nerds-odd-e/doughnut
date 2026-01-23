import MemoryTrackerPage from "@/pages/MemoryTrackerPage.vue"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect } from "vitest"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
    }),
  }
})

describe("MemoryTrackerPage", () => {
  const memoryTrackerId = 123

  it("fetches and displays recall prompts", async () => {
    const recallPrompt1 = makeMe.aRecallPrompt.please()
    const recallPrompt2 = makeMe.aRecallPrompt.please()
    const recallPrompts = [recallPrompt1, recallPrompt2]
    const memoryTracker = makeMe.aMemoryTracker.please()

    const getRecallPromptsSpy = mockSdkService(
      "getRecallPrompts",
      recallPrompts
    )
    const showMemoryTrackerSpy = mockSdkService(
      "showMemoryTracker",
      memoryTracker
    )
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(getRecallPromptsSpy).toHaveBeenCalledWith({
      path: { memoryTracker: memoryTrackerId },
    })
    expect(showMemoryTrackerSpy).toHaveBeenCalledWith({
      path: { memoryTracker: memoryTrackerId },
    })

    const memoryTrackerPageView = wrapper.findComponent({
      name: "MemoryTrackerPageView",
    })
    expect(memoryTrackerPageView.exists()).toBe(true)
    expect(memoryTrackerPageView.props("recallPrompts")).toEqual(recallPrompts)
    expect(memoryTrackerPageView.props("memoryTracker")).toEqual(memoryTracker)
    expect(memoryTrackerPageView.props("memoryTrackerId")).toBe(memoryTrackerId)
  })

  it("shows loading state while fetching", async () => {
    const recallPrompts = [makeMe.aRecallPrompt.please()]
    const memoryTracker = makeMe.aMemoryTracker.please()
    mockSdkService("getRecallPrompts", recallPrompts)
    mockSdkService("showMemoryTracker", memoryTracker)
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

  it("shows message when no recall prompts exist", async () => {
    const memoryTracker = makeMe.aMemoryTracker.please()
    mockSdkService("getRecallPrompts", [])
    mockSdkService("showMemoryTracker", memoryTracker)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("No recall prompts found")
  })

  it("shows error message when API call fails", async () => {
    vi.spyOn(MemoryTrackerController, "getRecallPrompts").mockResolvedValue(
      wrapSdkError("Error")
    )
    vi.spyOn(MemoryTrackerController, "showMemoryTracker").mockResolvedValue(
      wrapSdkError("Error")
    )
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Error loading memory tracker data")
  })

  it("displays multiple recall prompts ordered by creation time desc", async () => {
    const note = makeMe.aNote.please()
    const recallPrompt1 = makeMe.aRecallPrompt
      .withQuestionStem("Question 1")
      .please()
    const recallPrompt2 = makeMe.aRecallPrompt
      .withQuestionStem("Question 2")
      .please()
    const recallPrompt3 = makeMe.aRecallPrompt
      .withQuestionStem("Question 3")
      .please()
    recallPrompt1.note = note
    recallPrompt2.note = note
    recallPrompt3.note = note
    const recallPrompts = [recallPrompt3, recallPrompt2, recallPrompt1]
    const memoryTracker = makeMe.aMemoryTracker.please()

    mockSdkService("getRecallPrompts", recallPrompts)
    mockSdkService("showMemoryTracker", memoryTracker)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Question 3")
    expect(wrapper.text()).toContain("Question 2")
    expect(wrapper.text()).toContain("Question 1")
  })

  it("shows note under question only once", async () => {
    const note = makeMe.aNote.please()
    const recallPrompt1 = makeMe.aRecallPrompt.please()
    const recallPrompt2 = makeMe.aRecallPrompt.please()
    recallPrompt1.note = note
    recallPrompt2.note = note
    const recallPrompts = [recallPrompt1, recallPrompt2]
    const memoryTracker = makeMe.aMemoryTracker.please()

    mockSdkService("getRecallPrompts", recallPrompts)
    mockSdkService("showMemoryTracker", memoryTracker)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    const noteUnderQuestionComponents = wrapper.findAllComponents({
      name: "NoteUnderQuestion",
    })
    expect(noteUnderQuestionComponents.length).toBe(1)
  })

  it("shows question generated time", async () => {
    const note = makeMe.aNote.please()
    const questionGeneratedTime = new Date("2024-01-01T10:00:00Z").toISOString()
    const recallPrompt = makeMe.aRecallPrompt.please()
    recallPrompt.note = note
    recallPrompt.questionGeneratedTime = questionGeneratedTime
    const memoryTracker = makeMe.aMemoryTracker.please()

    mockSdkService("getRecallPrompts", [recallPrompt])
    mockSdkService("showMemoryTracker", memoryTracker)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Generated:")
    expect(wrapper.text()).toContain(
      new Date(questionGeneratedTime).toLocaleString()
    )
  })

  it("shows contested status for contested questions", async () => {
    const note = makeMe.aNote.please()
    const recallPrompt = makeMe.aRecallPrompt.please()
    recallPrompt.note = note
    recallPrompt.isContested = true
    const memoryTracker = makeMe.aMemoryTracker.please()

    mockSdkService("getRecallPrompts", [recallPrompt])
    mockSdkService("showMemoryTracker", memoryTracker)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Contested")
  })

  it("shows answer time for answered questions", async () => {
    const note = makeMe.aNote.please()
    const answerTime = new Date("2024-01-01T12:00:00Z").toISOString()
    const questionGeneratedTime = new Date("2024-01-01T10:00:00Z").toISOString()
    const recallPrompt = makeMe.aRecallPrompt.please()
    recallPrompt.note = note
    recallPrompt.questionGeneratedTime = questionGeneratedTime
    recallPrompt.answerTime = answerTime
    recallPrompt.answer = {
      id: 1,
      correct: true,
      choiceIndex: 0,
    }
    recallPrompt.predefinedQuestion = makeMe.aPredefinedQuestion.please()
    const memoryTracker = makeMe.aMemoryTracker.please()

    mockSdkService("getRecallPrompts", [recallPrompt])
    mockSdkService("showMemoryTracker", memoryTracker)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Generated:")
    expect(wrapper.text()).toContain(
      new Date(questionGeneratedTime).toLocaleString()
    )
    expect(wrapper.text()).toContain("Answered:")
    expect(wrapper.text()).toContain(new Date(answerTime).toLocaleString())
  })

  it("shows unanswered status for unanswered questions", async () => {
    const note = makeMe.aNote.please()
    const recallPrompt = makeMe.aRecallPrompt.please()
    recallPrompt.note = note
    const memoryTracker = makeMe.aMemoryTracker.please()

    mockSdkService("getRecallPrompts", [recallPrompt])
    mockSdkService("showMemoryTracker", memoryTracker)
    const wrapper = helper
      .component(MemoryTrackerPage)
      .withProps({ memoryTrackerId })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Unanswered")
  })
})
