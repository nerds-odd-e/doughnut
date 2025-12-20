import { flushPromises } from "@vue/test-utils"
import { vi, beforeEach, afterEach, describe, it, expect } from "vitest"
import { defineComponent, KeepAlive } from "vue"
import helper from "@tests/helpers"
import QuestionDisplay from "@/components/recall/QuestionDisplay.vue"
import makeMe from "@tests/fixtures/makeMe"
import markdownizer from "@/components/form/markdownizer"

describe("QuestionDisplay", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>
  let rafCallbacks: Array<FrameRequestCallback> = []

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
    rafCallbacks = []
    global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
      rafCallbacks.push(callback)
      return 1
    }) as unknown as typeof requestAnimationFrame
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  const flushRAF = () => {
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))
  }

  const setTime = (ms: number) => {
    performanceNowSpy.mockReturnValue(ms)
    vi.advanceTimersByTime(ms)
  }
  it("renders multiple choice question when choices are provided", async () => {
    const multipleChoicesQuestion = makeMe.aMultipleChoicesQuestion
      .withStem("What is the capital of France?")
      .withChoices(["Paris", "Berlin", "Rome"])
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()

    // Find choices using class selectors
    const choices = wrapper.findAll("li.choice button")
    expect(choices.length).toBe(3)
    expect(choices[0]?.text()).toBe("Paris")
    expect(choices[1]?.text()).toBe("Berlin")
    expect(choices[2]?.text()).toBe("Rome")
  })

  it("renders markdown in stem correctly", async () => {
    const markdownStem = "# What is 2 + 2?\n\nChoose the *correct* answer:"
    const multipleChoicesQuestion = makeMe.aMultipleChoicesQuestion
      .withStem(markdownStem)
      .withChoices(["4", "5", "6"])
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()

    const stem = wrapper.find("[data-test='stem']")
    const expectedHtml = markdownizer.markdownToHtml(markdownStem)
    // Remove all HTML tags to compare just the content
    const actualText = stem.text().replace(/\s+/g, " ").trim()
    const expectedText = expectedHtml
      .replace(/<[^>]*>/g, "")
      .replace(/\s+/g, " ")
      .trim()

    expect(actualText).toBe(expectedText)
  })

  it("renders markdown in choices correctly", async () => {
    const markdownChoices = [
      "**Bold** choice",
      "*Italic* choice",
      "~~Strikethrough~~ choice",
    ]
    const multipleChoicesQuestion = makeMe.aMultipleChoicesQuestion
      .withStem("Choose one:")
      .withChoices(markdownChoices)
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()

    const choices = wrapper.findAll("li.choice button")
    markdownChoices.forEach((choice, index) => {
      expect(choices[index]?.html()).toContain(
        markdownizer.markdownToHtml(choice)
      )
    })
  })

  it("includes thinking time in answer submission", async () => {
    const multipleChoicesQuestion = makeMe.aMultipleChoicesQuestion
      .withStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()
    flushRAF()
    setTime(5000)

    await wrapper.find("li.choice button").trigger("click")
    await flushPromises()

    const answerData = wrapper.emitted("answer")?.[0]?.[0] as {
      thinkingTimeMs?: number
    }
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(5000)
  })

  it("only records thinking time once per submission", async () => {
    const multipleChoicesQuestion = makeMe.aMultipleChoicesQuestion
      .withStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()
    flushRAF()
    setTime(1000)

    await wrapper.find("li.choice button").trigger("click")
    await flushPromises()

    const answerData = wrapper.emitted("answer")?.[0]?.[0] as {
      thinkingTimeMs?: number
    }
    expect(answerData?.thinkingTimeMs).toBe(1000)
  })

  it("pauses timer when component is deactivated (KeepAlive)", async () => {
    const multipleChoicesQuestion = makeMe.aMultipleChoicesQuestion
      .withStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    const WrapperComponent = defineComponent({
      components: { QuestionDisplay, KeepAlive },
      data() {
        return { show: true, question: multipleChoicesQuestion }
      },
      template: `
        <KeepAlive>
          <QuestionDisplay
            v-if="show"
            key="question"
            :multipleChoicesQuestion="question"
          />
        </KeepAlive>
      `,
    })

    const wrapper = helper.component(WrapperComponent).mount()
    await flushPromises()

    const questionComponent = wrapper.findComponent(QuestionDisplay)
    await questionComponent.vm.$nextTick()
    flushRAF()
    setTime(1000)

    await wrapper.setData({ show: false })
    await wrapper.vm.$nextTick()
    setTime(2000)

    await wrapper.setData({ show: true })
    await questionComponent.vm.$nextTick()
    flushRAF()
    setTime(2500)

    await questionComponent.find("li.choice button").trigger("click")
    await flushPromises()

    const answerData = questionComponent.emitted("answer")?.[0]?.[0] as {
      thinkingTimeMs?: number
    }
    // Should be ~1500ms (1000ms before + 500ms after), not 2500ms
    expect(answerData?.thinkingTimeMs).toBeLessThan(2000)
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(1000)
  })
})
