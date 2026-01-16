import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import SpellingQuestionDisplay from "@/components/recall/SpellingQuestionDisplay.vue"
import makeMe from "@tests/fixtures/makeMe"

describe("SpellingQuestionDisplay", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
    const recallPrompt = makeMe.aRecallPrompt
      .withQuestionType("SPELLING")
      .please()
    mockSdkService("askAQuestion", recallPrompt)
    const memoryTracker = makeMe.aMemoryTracker.please()
    // Add clozeDescription method to note for stem computation
    if (memoryTracker.note) {
      // @ts-expect-error - clozeDescription is a method on Note, not a property
      memoryTracker.note.clozeDescription = {
        clozeDetails: () => "<p>Spell the word 'cat'</p>\n",
      }
    }
    mockSdkService("showMemoryTracker", memoryTracker)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it("renders spelling question input form", async () => {
    const wrapper = helper
      .component(SpellingQuestionDisplay)
      .withProps({ memoryTrackerId: 1 })
      .mount()

    await flushPromises()

    expect(
      wrapper.find("input[placeholder='put your answer here']").exists()
    ).toBe(true)
    expect(wrapper.find("input[type='submit']").exists()).toBe(true)
  })

  it("emits answer event when form is submitted", async () => {
    let rafCallbacks: Array<FrameRequestCallback> = []
    vi.spyOn(window, "requestAnimationFrame").mockImplementation(
      (callback: FrameRequestCallback) => {
        rafCallbacks.push(callback)
        return 1
      }
    )

    const wrapper = helper
      .component(SpellingQuestionDisplay)
      .withProps({ memoryTrackerId: 1 })
      .mount()

    await flushPromises()
    // Flush RAF callbacks
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))

    // Set input value
    const input = wrapper.find("input[placeholder='put your answer here']")
    await input.setValue("cat")

    // Submit form
    await wrapper.find("form").trigger("submit")

    // Check emitted event
    const emitted = wrapper.emitted()
    expect(emitted.answer).toBeTruthy()
    expect(emitted.answer![0]).toEqual([
      {
        spellingAnswer: "cat",
        thinkingTimeMs: expect.any(Number),
        recallPromptId: expect.any(Number),
      },
    ])
  })

  it("includes thinking time in answer submission", async () => {
    let rafCallbacks: Array<FrameRequestCallback> = []
    vi.spyOn(window, "requestAnimationFrame").mockImplementation(
      (callback: FrameRequestCallback) => {
        rafCallbacks.push(callback)
        return 1
      }
    )

    const wrapper = helper
      .component(SpellingQuestionDisplay)
      .withProps({ memoryTrackerId: 1 })
      .mount()

    await flushPromises()
    // Flush RAF callbacks
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))

    performanceNowSpy.mockReturnValue(5000)
    vi.advanceTimersByTime(5000)

    const input = wrapper.find("input[placeholder='put your answer here']")
    await input.setValue("cat")
    await wrapper.find("form").trigger("submit")
    await flushPromises()

    const emitted = wrapper.emitted("answer")
    expect(emitted).toBeTruthy()
    expect(emitted?.[0]?.[0]).toHaveProperty("thinkingTimeMs")
    const answerData = emitted?.[0]?.[0] as {
      spellingAnswer?: string
      thinkingTimeMs?: number
      recallPromptId?: number
    }
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(5000)
    expect(answerData?.spellingAnswer).toBe("cat")
    expect(answerData?.recallPromptId).toBeDefined()
  })
})
