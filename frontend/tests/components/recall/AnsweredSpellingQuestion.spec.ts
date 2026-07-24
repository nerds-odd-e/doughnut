import AnsweredSpellingQuestion from "@/components/recall/AnsweredSpellingQuestion.vue"
import type {
  AnsweredQuestion,
  NoteRealm,
  User,
} from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { flushPromises } from "@vue/test-utils"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { beforeEach, describe, it, expect, vi } from "vitest"

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

function mountAnsweredSpellingQuestion(
  answeredQuestion: AnsweredQuestion,
  options: {
    currentUser?: User
    seedRealms?: NoteRealm[]
    withRouter?: boolean
  } = {}
) {
  let chain = helper
    .component(AnsweredSpellingQuestion)
    .withCleanStorage()
    .withProps({ answeredQuestion })
  if (options.withRouter) {
    chain = chain.withRouter()
  }
  if (options.currentUser) {
    chain = chain.withCurrentUser(options.currentUser)
  }
  if (options.seedRealms) {
    for (const realm of options.seedRealms) {
      useStorageAccessor().value.refreshNoteRealm(realm)
    }
  }
  return chain.mount({
    attachTo: document.body,
    global: {
      stubs: {
        NoteShow: noteShowStub,
        NoteUnderQuestion: true,
        ViewMemoryTrackerLink: true,
      },
    },
  })
}

function accidentalMatchWithTwoMatchedNotes() {
  const reviewedRealm = makeMe.aNoteRealm.title("Reviewed Note").please()
  const matchedA = makeMe.aNoteRealm.title("Matched A").please()
  matchedA.id = 10
  matchedA.note.id = 10
  matchedA.note.noteTopology.id = 10
  matchedA.note.noteTopology.title = "Matched A"
  const matchedB = makeMe.aNoteRealm.title("Matched B").please()
  matchedB.id = 20
  matchedB.note.id = 20
  matchedB.note.noteTopology.id = 20
  matchedB.note.noteTopology.title = "Matched B"

  const answeredQuestion = makeMe.anAnsweredQuestion
    .withNote(reviewedRealm.note)
    .spelling()
    .answerCorrect(false)
    .withAnswer({
      id: 1,
      correct: false,
      spellingAnswer: "matched a",
      outcome: "ACCIDENTAL_MATCH",
      matchedNoteId: 10,
    })
    .withMatchedNotes([matchedA.note.noteTopology, matchedB.note.noteTopology])
    .please()

  return { answeredQuestion, reviewedRealm, matchedA, matchedB }
}

describe("AnsweredSpellingQuestion", () => {
  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

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

    it("shows one Link to this note control per matched note when writable", async () => {
      const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
        accidentalMatchWithTwoMatchedNotes()
      const wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
        currentUser: makeMe.aUser.please(),
        seedRealms: [reviewedRealm, matchedA, matchedB],
      })
      await flushPromises()

      expect(
        wrapper.find('[data-testid="link-to-matched-note-10"]').exists()
      ).toBe(true)
      expect(
        wrapper.find('[data-testid="link-to-matched-note-20"]').exists()
      ).toBe(true)
      expect(
        wrapper.findAll('[data-testid^="link-to-matched-note-"]')
      ).toHaveLength(2)
    })

    it("opens preselected Link to: without a search field", async () => {
      const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
        accidentalMatchWithTwoMatchedNotes()
      const wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
        currentUser: makeMe.aUser.please(),
        seedRealms: [reviewedRealm, matchedA, matchedB],
        withRouter: true,
      })
      await flushPromises()

      await wrapper
        .find('[data-testid="link-to-matched-note-10"]')
        .trigger("click")
      await flushPromises()

      expect(document.body.textContent).toContain("Link to:")
      expect(document.body.textContent).toContain("Matched A")
      expect(document.body.querySelector("input")).toBeNull()
    })

    it("omits link CTAs when reviewed notebook is readonly", async () => {
      const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
        accidentalMatchWithTwoMatchedNotes()
      reviewedRealm.notebookRealm.readonly = true
      const wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
        currentUser: makeMe.aUser.please(),
        seedRealms: [reviewedRealm, matchedA, matchedB],
      })
      await flushPromises()

      expect(
        wrapper.find('[data-testid="matched-notes-section"]').exists()
      ).toBe(true)
      expect(
        wrapper.findAll('[data-testid^="link-to-matched-note-"]')
      ).toHaveLength(0)
    })

    it("omits link CTAs until reviewed and matched realms are loaded", async () => {
      mockSdkServiceWithImplementation(
        NoteController,
        "showNote",
        () =>
          new Promise(() => {
            // Intentionally never settles — realms stay unloaded for this gate.
          })
      )
      const { answeredQuestion } = accidentalMatchWithTwoMatchedNotes()
      const wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
        currentUser: makeMe.aUser.please(),
      })
      await flushPromises()

      expect(
        wrapper.find('[data-testid="matched-notes-section"]').exists()
      ).toBe(true)
      expect(
        wrapper.findAll('[data-testid^="link-to-matched-note-"]')
      ).toHaveLength(0)
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
