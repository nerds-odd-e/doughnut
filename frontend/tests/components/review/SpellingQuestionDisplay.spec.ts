import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import SpellingQuestionDisplay from "@/components/review/SpellingQuestionDisplay.vue"
import makeMe from "@tests/fixtures/makeMe"

describe("SpellingQuestionDisplay", () => {
  it("renders spelling question input form", async () => {
    const bareQuestion = makeMe.aBareQuestion
      .withStem("Spell the word 'cat'")
      .please()

    const wrapper = helper
      .component(SpellingQuestionDisplay)
      .withProps({ bareQuestion })
      .mount()

    await flushPromises()

    // Check for input form elements
    expect(
      wrapper.find("input[placeholder='put your answer here']").exists()
    ).toBe(true)
    expect(wrapper.find("input[type='submit']").exists()).toBe(true)
  })

  it("emits answer event when form is submitted", async () => {
    const bareQuestion = makeMe.aBareQuestion
      .withStem("Spell the word 'cat'")
      .please()

    const wrapper = helper
      .component(SpellingQuestionDisplay)
      .withProps({ bareQuestion })
      .mount()

    await flushPromises()

    // Set input value
    const input = wrapper.find("input[placeholder='put your answer here']")
    await input.setValue("cat")

    // Submit form
    await wrapper.find("form").trigger("submit")

    // Check emitted event
    const emitted = wrapper.emitted()
    expect(emitted.answer).toBeTruthy()
    expect(emitted.answer![0]).toEqual([{ spellingAnswer: "cat" }])
  })
})
