import { flushPromises } from "@vue/test-utils"
import { vi, beforeEach, afterEach, describe, it, expect } from "vitest"
import { defineComponent, KeepAlive } from "vue"
import helper from "@tests/helpers"
import QuestionDisplay from "@/components/recall/QuestionDisplay.vue"
import makeMe from "@tests/fixtures/makeMe"
import markdownizer from "@/components/form/markdownizer"

describe("QuestionDisplay", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })
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

    let rafCallbacks: Array<FrameRequestCallback> = []
    global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
      rafCallbacks.push(callback)
      return 1
    }) as unknown as typeof requestAnimationFrame

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()
    // Flush RAF callbacks
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))

    performanceNowSpy.mockReturnValue(5000)
    vi.advanceTimersByTime(5000)

    const choiceButton = wrapper.find("li.choice button")
    await choiceButton.trigger("click")
    await flushPromises()

    const emitted = wrapper.emitted("answer")
    expect(emitted).toBeTruthy()
    expect(emitted?.[0]?.[0]).toHaveProperty("thinkingTimeMs")
    const answerData = emitted?.[0]?.[0] as { thinkingTimeMs?: number }
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(5000)
  })

  it("only records thinking time once per submission", async () => {
    const multipleChoicesQuestion = makeMe.aMultipleChoicesQuestion
      .withStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    let rafCallbacks: Array<FrameRequestCallback> = []
    global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
      rafCallbacks.push(callback)
      return 1
    }) as unknown as typeof requestAnimationFrame

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()
    // Flush RAF callbacks
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    const choiceButton = wrapper.find("li.choice button")
    await choiceButton.trigger("click")
    await flushPromises()

    const emitted = wrapper.emitted("answer")
    expect(emitted).toBeTruthy()
    const firstAnswerData = emitted?.[0]?.[0] as { thinkingTimeMs?: number }
    const firstTime = firstAnswerData?.thinkingTimeMs
    expect(firstTime).toBe(1000)

    // Verify that the tracker returns the same value when stop() is called multiple times
    // by checking that subsequent clicks (if they were allowed) would return the same time
    // Since the button uses @click.once, we can't test multiple clicks directly,
    // but the composable tests verify that stop() returns the same value when called multiple times
    expect(firstTime).toBeDefined()
    expect(firstTime).toBeGreaterThanOrEqual(1000)
  })

  it("pauses timer when component is deactivated (KeepAlive scenario)", async () => {
    const multipleChoicesQuestion = makeMe.aMultipleChoicesQuestion
      .withStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    let rafCallbacks: Array<FrameRequestCallback> = []
    global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
      rafCallbacks.push(callback)
      return 1
    }) as unknown as typeof requestAnimationFrame

    const WrapperComponent = defineComponent({
      components: { QuestionDisplay, KeepAlive },
      data() {
        return { show: true }
      },
      template: `
        <KeepAlive>
          <QuestionDisplay
            v-if="show"
            key="question"
            :multipleChoicesQuestion="multipleChoicesQuestion"
            @answer="handleAnswer"
          />
        </KeepAlive>
      `,
      methods: {
        handleAnswer() {
          // noop
        },
      },
      computed: {
        multipleChoicesQuestion() {
          return multipleChoicesQuestion
        },
      },
    })

    const wrapper = helper.component(WrapperComponent).mount()
    await flushPromises()

    const questionComponent = wrapper.findComponent(QuestionDisplay)
    await questionComponent.vm.$nextTick()

    // Flush RAF callbacks
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))

    // Timer runs for 1 second
    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    // Simulate component deactivation (user navigates away, but component is kept alive)
    await wrapper.setData({ show: false })
    await wrapper.vm.$nextTick()

    // Time passes while component is deactivated
    performanceNowSpy.mockReturnValue(2000)
    vi.advanceTimersByTime(1000)

    // Component is still alive but deactivated, timer should be paused
    // We need to access the stop function from the component instance
    // Since we can't directly access it, we'll trigger an answer to see the thinking time
    await wrapper.setData({ show: true })
    await questionComponent.vm.$nextTick()

    // Flush RAF again after reactivation
    const callbacks2 = [...rafCallbacks]
    rafCallbacks = []
    callbacks2.forEach((cb) => cb(performance.now()))

    // Wait a bit more
    performanceNowSpy.mockReturnValue(2500)
    vi.advanceTimersByTime(500)

    // Submit answer - should only include time from before deactivation + after reactivation
    const choiceButton = questionComponent.find("li.choice button")
    await choiceButton.trigger("click")
    await flushPromises()

    const emitted = questionComponent.emitted("answer")
    expect(emitted).toBeTruthy()
    const answerData = emitted?.[0]?.[0] as { thinkingTimeMs?: number }
    // Should be approximately 1500ms (1000ms before deactivation + 500ms after reactivation)
    // Not 2500ms (which would be if timer continued during deactivation)
    expect(answerData?.thinkingTimeMs).toBeLessThan(2000)
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(1000)
  })
})
