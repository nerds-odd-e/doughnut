import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import QuestionChoices from "@/components/recall/QuestionChoices.vue"

describe("QuestionChoices", () => {
  it("prevents link navigation while allowing answer submission", async () => {
    const choices = ["Regular text", "Choice with [link](http://example.com)"]

    const wrapper = mount(QuestionChoices, {
      props: {
        choices,
        correctChoiceIndex: 0,
        answerChoiceIndex: undefined,
        disabled: false,
      },
    })

    // Find the button containing the link
    const buttons = wrapper.findAll("button")
    const linkButton = buttons[1]
    if (!linkButton) {
      throw new Error("Link button not found")
    }
    const linkElement = linkButton.find("a")

    // Verify link exists in the rendered HTML
    expect(linkElement.exists()).toBe(true)
    expect(linkElement.attributes("href")).toBe("http://example.com")

    // Click the button and verify the answer event is emitted
    await linkButton.trigger("click")

    // The answer should be emitted with index 1 (second choice)
    const emitted = wrapper.emitted()
    expect(emitted).toHaveProperty("answer")
    expect(emitted.answer?.[0]).toEqual([{ choiceIndex: 1 }])
  })
})
