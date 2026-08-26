import { flushPromises } from "@vue/test-utils"
import { describe, it, expect } from "vitest"
import helper from "@tests/helpers"
import QuestionDisplay from "@/components/recall/QuestionDisplay.vue"
import makeMe from "donut-test-fixtures/makeMe"
import markdownizer from "@/components/form/markdownizer"
import type { Answer } from "@generated/donut-backend-api"
import { questionDisplayProps } from "./questionDisplayTestSupport"

describe("QuestionDisplay", () => {
  it("renders multiple choice question when choices are provided", async () => {
    const mcq = makeMe.anMcq
      .withQuestionStem("What is the capital of France?")
      .withChoices(["Paris", "Berlin", "Rome"])
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps(questionDisplayProps(mcq))
      .mount()

    await flushPromises()

    const choices = wrapper.findAll("li.choice button")
    expect(choices.length).toBe(3)
    expect(choices[0]?.text()).toBe("Paris")
    expect(choices[1]?.text()).toBe("Berlin")
    expect(choices[2]?.text()).toBe("Rome")
  })

  it("renders markdown in stem correctly", async () => {
    const markdownStem = "# What is 2 + 2?\n\nChoose the *correct* answer:"
    const mcq = makeMe.anMcq
      .withQuestionStem(markdownStem)
      .withChoices(["4", "5", "6"])
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps(questionDisplayProps(mcq))
      .mount()

    await flushPromises()

    const stem = wrapper.find("[data-test='stem']")
    const expectedHtml = markdownizer.markdownToHtml(markdownStem)
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
    const mcq = makeMe.anMcq
      .withQuestionStem("Choose one:")
      .withChoices(markdownChoices)
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps(questionDisplayProps(mcq))
      .mount()

    await flushPromises()

    const choices = wrapper.findAll("li.choice button")
    markdownChoices.forEach((choice, index) => {
      expect(choices[index]?.html()).toContain(
        markdownizer.markdownToHtml(choice)
      )
    })
  })

  it("shows designer notes after answer when tested focus or rationale exist", async () => {
    const mcq = makeMe.anMcq
      .withQuestionStem("Q")
      .withChoices(["A", "B"])
      .please()
    const answer = {
      id: 1,
      correct: true,
      choiceIndex: 0,
    } satisfies Answer

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({
        ...questionDisplayProps(mcq),
        correctChoiceIndex: 0,
        answer,
        testedFocus: "Tests recall of the capital.",
        validationRationale:
          "Paris is correct; Berlin and Rome are wrong capitals.",
      })
      .mount()

    await flushPromises()

    const notes = wrapper.find("[data-test='question-ai-notes']")
    expect(notes.exists()).toBe(true)
    expect(notes.text()).toContain("Tests recall of the capital.")
    expect(notes.text()).toContain("Paris is correct")
  })

  it("does not show designer notes before answering", async () => {
    const mcq = makeMe.anMcq
      .withQuestionStem("Q")
      .withChoices(["A", "B"])
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({
        ...questionDisplayProps(mcq),
        testedFocus: "Secret",
        validationRationale: "Also secret",
      })
      .mount()

    await flushPromises()

    expect(wrapper.find("[data-test='question-ai-notes']").exists()).toBe(false)
  })
})
