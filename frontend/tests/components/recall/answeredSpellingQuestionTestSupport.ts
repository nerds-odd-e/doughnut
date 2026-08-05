import AnsweredSpellingQuestion from "@/components/recall/AnsweredSpellingQuestion.vue"
import type {
  AnsweredQuestion,
  NoteRealm,
  User,
} from "@generated/doughnut-backend-api"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import helper from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { vi } from "vitest"

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

export function mountAnsweredSpellingQuestion(
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

export function accidentalMatchWithTwoMatchedNotes() {
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
