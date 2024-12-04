import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import QuestionDisplay from "@/components/review/QuestionDisplay.vue"
import makeMe from "@tests/fixtures/makeMe"

describe("QuestionDisplay", () => {
  it("renders multiple choice question when choices are provided", async () => {
    const bareQuestion = makeMe.aBareQuestion
      .withStem("What is the capital of France?")
      .withChoices(["Paris", "Berlin", "Rome"])
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ bareQuestion })
      .mount()

    await flushPromises()

    // Find choices using class selectors
    const choices = wrapper.findAll("li.choice button")
    expect(choices.length).toBe(3)
    expect(choices[0]?.text()).toBe("Paris")
    expect(choices[1]?.text()).toBe("Berlin")
    expect(choices[2]?.text()).toBe("Rome")
  })

  it("renders spelling question when no choices are provided", async () => {
    const bareQuestion = makeMe.aBareQuestion
      .withStem("Spell the word 'cat'")
      .please()

    const wrapper = helper
      .component(QuestionDisplay)
      .withProps({ bareQuestion })
      .mount()

    await flushPromises()

    // Check for input form elements
    expect(
      wrapper.find("input[placeholder='put your answer here']").exists()
    ).toBe(true)
    expect(wrapper.find("input[type='submit']").exists()).toBe(true)
  })
})
