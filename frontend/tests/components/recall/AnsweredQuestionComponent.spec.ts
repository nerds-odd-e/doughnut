import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService, mockShowNote } from "@tests/helpers"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import makeMe from "donut-test-fixtures/makeMe"
import { refinementLayoutItems } from "./noteRefinementTestSupport"
import { afterEach, beforeEach, describe, it, expect } from "vitest"

describe("AnsweredQuestionComponent", () => {
  let wrapper: VueWrapper | undefined

  beforeEach(() => {
    mockShowNote()
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
    teardownGlobalClientForTesting()
    document.body.innerHTML = ""
  })

  describe("note under question", () => {
    it("renders note under question and updates when answeredQuestion prop changes", async () => {
      const note1 = makeMe.aNote.title("First Note").please()
      const note2 = makeMe.aNote.title("Second Note").please()
      const answeredQuestion1 = makeMe.anAnsweredQuestion
        .withNote(note1)
        .please()
      const answeredQuestion2 = makeMe.anAnsweredQuestion
        .withNote(note2)
        .please()

      wrapper = helper
        .component(AnsweredQuestionComponent)
        .withRouter()
        .withProps({
          answeredQuestion: answeredQuestion1,
          conversationButton: false,
        })
        .mount()

      await flushPromises()
      expect(wrapper.text()).toContain("First Note")

      await wrapper.setProps({ answeredQuestion: answeredQuestion2 })
      await flushPromises()
      expect(wrapper.text()).toContain("Second Note")
      expect(wrapper.text()).not.toContain("First Note")
    })
  })

  describe("refine note", () => {
    beforeEach(() => {
      mockSdkService(AiController, "generateRefinementSuggestions", {
        items: refinementLayoutItems([]),
      })
    })

    function mountWithSeededNote(noteRealm: NoteRealm) {
      const answeredQuestion = makeMe.anAnsweredQuestion
        .withNote(noteRealm.note)
        .please()
      const chain = helper
        .component(AnsweredQuestionComponent)
        .withRouter()
        .withCleanStorage()
      useStorageAccessor().value.refreshNoteRealm(noteRealm)
      return chain
        .withProps({ answeredQuestion, conversationButton: false })
        .mount({ attachTo: document.body })
    }

    it("shows Refine note, opens refine modal, and passes MCQ context when present", async () => {
      const mcq = makeMe.anMcq
        .withQuestionStem("What is the capital of France?")
        .withChoices(["Paris", "London", "Berlin"])
        .correctAnswerIndex(0)
        .testedFocus("capital city")
        .please()

      const generateSpy = mockSdkService(
        AiController,
        "generateRefinementSuggestions",
        {
          items: refinementLayoutItems([]),
        }
      )

      const noteRealm = makeMe.aNoteRealm
        .title("Contentful Note")
        .content("Paris is the capital of France. Other fact.")
        .please()
      const answeredQuestion = makeMe.anAnsweredQuestion
        .withNote(noteRealm.note)
        .withMcq(mcq)
        .please()
      const chain = helper
        .component(AnsweredQuestionComponent)
        .withRouter()
        .withCleanStorage()
      useStorageAccessor().value.refreshNoteRealm(noteRealm)
      wrapper = chain
        .withProps({ answeredQuestion, conversationButton: false })
        .mount({ attachTo: document.body })
      await flushPromises()

      expect(wrapper.text()).toContain("View Memory Tracker")
      const refineButton = wrapper.find('[data-test="open-refine-note-modal"]')
      expect(refineButton.exists()).toBe(true)
      expect(refineButton.text()).toContain("Refine note")

      await refineButton.trigger("click")
      await flushPromises()

      const modal = document.querySelector('[data-test="refine-note-modal"]')
      expect(modal).not.toBeNull()
      expect(modal!.classList.contains("daisy-modal-open")).toBe(true)
      expect(
        document.querySelector('[data-test-id="refinement-layout-empty"]')
      ).not.toBeNull()
      expect(generateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { note: noteRealm.note.id },
          body: {
            stem: "What is the capital of France?",
            choices: ["Paris", "London", "Berlin"],
            correctAnswerIndex: 0,
            testedFocus: "capital city",
          },
        })
      )
    })

    it("hides Refine note when recalled note content is blank", async () => {
      const noteRealm = makeMe.aNoteRealm.content("   ").please()

      wrapper = mountWithSeededNote(noteRealm)
      await flushPromises()

      expect(wrapper.text()).toContain("View Memory Tracker")
      expect(
        wrapper.find('[data-test="open-refine-note-modal"]').exists()
      ).toBe(false)
    })
  })
})
