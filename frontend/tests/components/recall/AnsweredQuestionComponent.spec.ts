import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { describe, it, expect } from "vitest"

describe("AnsweredQuestionComponent", () => {
  describe("note under question", () => {
    it("renders note under question when note is present", async () => {
      const note = makeMe.aNote.title("Test Note Title").please()
      const answeredQuestion = makeMe.anAnsweredQuestion.withNote(note).please()

      const wrapper = helper
        .component(AnsweredQuestionComponent)
        .withProps({ answeredQuestion, conversationButton: false })
        .mount()

      await flushPromises()

      expect(wrapper.text()).toContain("Test Note Title")
    })

    it("updates note display when answeredQuestion prop changes", async () => {
      const note1 = makeMe.aNote.title("First Note").please()
      const note2 = makeMe.aNote.title("Second Note").please()
      const answeredQuestion1 = makeMe.anAnsweredQuestion
        .withNote(note1)
        .please()
      const answeredQuestion2 = makeMe.anAnsweredQuestion
        .withNote(note2)
        .please()

      const wrapper = helper
        .component(AnsweredQuestionComponent)
        .withProps({
          answeredQuestion: answeredQuestion1,
          conversationButton: false,
        })
        .mount()

      await flushPromises()

      // Verify first note is displayed
      expect(wrapper.text()).toContain("First Note")
      expect(wrapper.text()).not.toContain("Second Note")

      // Update prop to second answered question
      await wrapper.setProps({ answeredQuestion: answeredQuestion2 })
      await flushPromises()

      // Verify second note is now displayed
      expect(wrapper.text()).toContain("Second Note")
      expect(wrapper.text()).not.toContain("First Note")
    })
  })
})
