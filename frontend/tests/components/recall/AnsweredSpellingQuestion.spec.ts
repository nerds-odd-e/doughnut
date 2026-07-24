import AnsweredSpellingQuestion from "@/components/recall/AnsweredSpellingQuestion.vue"
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, it, expect, vi } from "vitest"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
    }),
  }
})

const noteShowStub = {
  name: "NoteShow",
  props: ["noteId", "expandChildren"],
  template:
    '<div data-testid="note-show-stub" :data-note-id="noteId" :data-expand-children="String(expandChildren)" />',
}

function mountAnsweredSpellingQuestion(answeredQuestion: AnsweredQuestion) {
  return helper
    .component(AnsweredSpellingQuestion)
    .withProps({ answeredQuestion })
    .mount({
      global: {
        stubs: {
          NoteShow: noteShowStub,
          NoteUnderQuestion: true,
          ViewMemoryTrackerLink: true,
        },
      },
    })
}

describe("AnsweredSpellingQuestion", () => {
  describe("accidental match result", () => {
    it("shows distinct alert copy and a matched notes section with NoteShows", async () => {
      const reviewed = makeMe.aNote.title("Reviewed Note").please()
      const matchedA = makeMe.aNote.title("Matched A").please()
      matchedA.id = 10
      matchedA.noteTopology.id = 10
      const matchedB = makeMe.aNote.title("Matched B").please()
      matchedB.id = 20
      matchedB.noteTopology.id = 20

      const answeredQuestion = makeMe.anAnsweredQuestion
        .withNote(reviewed)
        .spelling()
        .answerCorrect(false)
        .withAnswer({
          id: 1,
          correct: false,
          spellingAnswer: "matched a",
          outcome: "ACCIDENTAL_MATCH",
          matchedNoteId: 10,
        })
        .withMatchedNotes([matchedA.noteTopology, matchedB.noteTopology])
        .please()

      const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
      await flushPromises()

      expect(wrapper.text()).toContain(
        "Your answer `matched a` names another note — not correct for this review."
      )
      expect(wrapper.text()).not.toContain(
        "Your answer `matched a` is incorrect."
      )

      const matchedSection = wrapper.find(
        '[data-testid="matched-notes-section"]'
      )
      expect(matchedSection.exists()).toBe(true)
      expect(matchedSection.text()).toContain("Matched note(s)")

      const matchedShows = matchedSection.findAllComponents({
        name: "NoteShow",
      })
      expect(matchedShows).toHaveLength(2)
      expect(matchedShows[0].props("noteId")).toBe(10)
      expect(matchedShows[0].props("expandChildren")).toBe(false)
      expect(matchedShows[1].props("noteId")).toBe(20)
      expect(matchedShows[1].props("expandChildren")).toBe(false)
    })

    it("omits matched notes section when matchedNotes is empty", async () => {
      const reviewed = makeMe.aNote.title("Reviewed Note").please()
      const answeredQuestion = makeMe.anAnsweredQuestion
        .withNote(reviewed)
        .spelling()
        .withAnswer({
          id: 1,
          correct: false,
          spellingAnswer: "ghost",
          outcome: "ACCIDENTAL_MATCH",
        })
        .withMatchedNotes([])
        .please()

      const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
      await flushPromises()

      expect(wrapper.text()).toContain(
        "Your answer `ghost` names another note — not correct for this review."
      )
      expect(
        wrapper.find('[data-testid="matched-notes-section"]').exists()
      ).toBe(false)
    })
  })

  describe("plain wrong result", () => {
    it("keeps incorrect alert copy and omits matched notes section", async () => {
      const reviewed = makeMe.aNote.title("Reviewed Note").please()
      const answeredQuestion = makeMe.anAnsweredQuestion
        .withNote(reviewed)
        .spelling()
        .withAnswer({
          id: 1,
          correct: false,
          spellingAnswer: "typo",
        })
        .please()

      const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
      await flushPromises()

      expect(wrapper.text()).toContain("Your answer `typo` is incorrect.")
      expect(wrapper.text()).not.toContain("names another note")
      expect(
        wrapper.find('[data-testid="matched-notes-section"]').exists()
      ).toBe(false)
    })
  })
})
