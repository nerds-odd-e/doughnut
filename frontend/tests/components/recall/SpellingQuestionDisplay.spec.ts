import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  expectSoftKeyboardPrimerIsFocused,
  expectSoftKeyboardPrimerIsNotFocused,
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import {
  captureRequestAnimationFrame,
  mockSpellingQuestionServices,
  mountSpellingQuestionDisplay,
  submitSpellingAnswer,
} from "./spellingQuestionDisplayTestSupport"

describe("SpellingQuestionDisplay", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined
  let rafCallbacks: FrameRequestCallback[]

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
    rafCallbacks = captureRequestAnimationFrame()
    mockSpellingQuestionServices()
  })

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    Object.defineProperty(document, "hidden", { value: false, writable: true })
    vi.useRealTimers()
    vi.restoreAllMocks()
    document.body.innerHTML = ""
  })

  describe("soft keyboard primer", () => {
    beforeEach(() => {
      mountSoftKeyboardPrimer()
    })

    it.each([
      { nextIsSpelling: true, expectPrimerFocused: true },
      { nextIsSpelling: false, expectPrimerFocused: false },
    ])(
      "primer focus on submit when nextIsSpelling=$nextIsSpelling",
      async ({ nextIsSpelling, expectPrimerFocused }) => {
        matchMediaSpy = mockCoarsePointer(true)
        expect(softKeyboardPrimerElement()).toBeTruthy()

        const wrapper = await mountSpellingQuestionDisplay(
          { memoryTrackerId: 1, nextIsSpelling },
          { attachTo: document.body, rafCallbacks }
        )
        await submitSpellingAnswer(wrapper)

        if (expectPrimerFocused) {
          expectSoftKeyboardPrimerIsFocused()
        } else {
          expectSoftKeyboardPrimerIsNotFocused()
        }
        wrapper.unmount()
      }
    )
  })

  it("emits answer with thinking time on submit", async () => {
    const wrapper = await mountSpellingQuestionDisplay(
      { memoryTrackerId: 1 },
      { rafCallbacks }
    )

    performanceNowSpy.mockReturnValue(5000)
    vi.advanceTimersByTime(5000)
    await submitSpellingAnswer(wrapper)

    const answerData = wrapper.emitted("answer")?.[0]?.[0] as {
      spellingAnswer?: string
      thinkingTimeMs?: number
      recallPromptId?: number
    }
    expect(answerData?.spellingAnswer).toBe("cat")
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(5000)
    expect(answerData?.recallPromptId).toBeDefined()
  })

  it("emits answer only once when submit is triggered multiple times", async () => {
    const wrapper = await mountSpellingQuestionDisplay(
      { memoryTrackerId: 1 },
      { rafCallbacks }
    )
    await wrapper
      .find("input[placeholder='put your answer here']")
      .setValue("cat")
    const form = wrapper.find("form")
    await form.trigger("submit")
    await form.trigger("submit")
    await form.trigger("submit")

    expect(wrapper.emitted("answer")).toHaveLength(1)
  })

  it.each([
    {
      case: "document hidden",
      trigger: async () => {
        Object.defineProperty(document, "hidden", {
          value: true,
          writable: true,
        })
        document.dispatchEvent(new Event("visibilitychange"))
      },
      expectMessage: true,
    },
    {
      case: "window blur",
      trigger: async () => {
        window.dispatchEvent(new Event("blur"))
      },
      expectMessage: false,
    },
  ])("shows inactive mask when $case", async ({ trigger, expectMessage }) => {
    const wrapper = await mountSpellingQuestionDisplay(
      { memoryTrackerId: 1 },
      { rafCallbacks }
    )
    expect(wrapper.find("[data-test='inactive-recall-mask']").exists()).toBe(
      false
    )

    await trigger()
    await wrapper.vm.$nextTick()

    const mask = wrapper.find("[data-test='inactive-recall-mask']")
    expect(mask.exists()).toBe(true)
    if (expectMessage) {
      expect(mask.text()).toContain("Focus to activate")
    }
  })

  it("disables submit button after form is submitted", async () => {
    const wrapper = await mountSpellingQuestionDisplay(
      { memoryTrackerId: 1 },
      { rafCallbacks }
    )
    const submitButton = wrapper.find("input[type='submit']")
    expect(submitButton.attributes("disabled")).toBeUndefined()

    await submitSpellingAnswer(wrapper)

    expect(submitButton.attributes("disabled")).toBeDefined()
  })
})
