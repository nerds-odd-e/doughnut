import { flushPromises } from "@vue/test-utils"
import { vi, beforeEach, afterEach } from "vitest"
import helper from "@tests/helpers"
import QuestionDisplay from "@/components/review/QuestionDisplay.vue"
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

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()
    await new Promise((resolve) => requestAnimationFrame(resolve))

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

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ multipleChoicesQuestion })
      .mount()

    await flushPromises()
    await new Promise((resolve) => requestAnimationFrame(resolve))

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    const choiceButton = wrapper.find("li.choice button")
    await choiceButton.trigger("click")
    await flushPromises()

    const emitted = wrapper.emitted("answer")
    expect(emitted).toBeTruthy()
    const firstAnswerData = emitted?.[0]?.[0] as { thinkingTimeMs?: number }
    const firstTime = firstAnswerData?.thinkingTimeMs

    await choiceButton.trigger("click")
    await flushPromises()

    const secondEmitted = wrapper.emitted("answer")
    const secondAnswerData = secondEmitted?.[1]?.[0] as {
      thinkingTimeMs?: number
    }
    expect(secondAnswerData?.thinkingTimeMs).toBe(firstTime)
  })
})
