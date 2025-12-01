import { describe, it, expect, beforeEach, afterEach } from "vitest"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"
import SpellingQuestionComponent from "@/components/review/SpellingQuestionComponent.vue"

describe("SpellingQuestionDisplay", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
    mockSdkService("getSpellingQuestion", { stem: "Spell the word 'cat'" })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it("renders spelling question input form", async () => {
    const wrapper = helper
      .component(SpellingQuestionComponent)
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
    global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
      rafCallbacks.push(callback)
      return 1
    }) as unknown as typeof requestAnimationFrame

    const wrapper = helper
      .component(SpellingQuestionComponent)
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
      { spellingAnswer: "cat", thinkingTimeMs: expect.any(Number) },
    ])
  })

  it("includes thinking time in answer submission", async () => {
    let rafCallbacks: Array<FrameRequestCallback> = []
    global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
      rafCallbacks.push(callback)
      return 1
    }) as unknown as typeof requestAnimationFrame

    const wrapper = helper
      .component(SpellingQuestionComponent)
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
    }
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(5000)
    expect(answerData?.spellingAnswer).toBe("cat")
  })
})
