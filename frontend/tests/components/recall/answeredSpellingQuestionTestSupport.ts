import AnsweredSpellingQuestion from "@/components/recall/AnsweredSpellingQuestion.vue"
import type {
  AnsweredQuestion,
  NoteRealm,
  User,
} from "@generated/donut-backend-api"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"
import helper from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises, type VueWrapper } from "@vue/test-utils"

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
  } = {}
) {
  let chain = helper
    .component(AnsweredSpellingQuestion)
    .withCleanStorage()
    .withProps({ answeredQuestion })
    .withRouter()
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

export async function openResolveAccidentalMatch(wrapper: VueWrapper) {
  await wrapper
    .find('[data-testid="resolve-accidental-match"]')
    .trigger("click")
  await flushPromises()
}

export function accidentalMatchWithTwoMatchedNotes(
  options: { notebookNames?: [string, string]; reviewedReadonly?: boolean } = {}
) {
  let reviewedBuilder = makeMe.aNoteRealm.title("Reviewed Note")
  if (options.reviewedReadonly) {
    reviewedBuilder = reviewedBuilder.readonly()
  }
  const reviewedRealm = reviewedBuilder.please()
  let matchedABuilder = makeMe.aNoteRealm.id(10).title("Matched A")
  let matchedBBuilder = makeMe.aNoteRealm.id(20).title("Matched B")
  if (options.notebookNames) {
    matchedABuilder = matchedABuilder.notebookName(options.notebookNames[0])
    matchedBBuilder = matchedBBuilder.notebookName(options.notebookNames[1])
  }
  const matchedA = matchedABuilder.please()
  const matchedB = matchedBBuilder.please()
  const answeredQuestion = makeMe.anAnsweredQuestion
    .withNote(reviewedRealm.note)
    .accidentalMatch("matched a", [
      matchedA.note.noteTopology,
      matchedB.note.noteTopology,
    ])
    .please()
  return { answeredQuestion, reviewedRealm, matchedA, matchedB }
}

export function reviewedRealmDeclaringMatch(
  reviewedRealm: NoteRealm,
  matchedRealm: NoteRealm,
  property: "overlaps" | "aliases"
) {
  const token = buildWikiLinkText(
    {
      noteTopology: matchedRealm.note.noteTopology,
      notebookId: matchedRealm.notebookRealm.notebook.id,
      notebookName: matchedRealm.notebookRealm.notebook.name,
    },
    { notebookId: reviewedRealm.notebookRealm.notebook.id }
  )
  return makeMe.aNoteRealm
    .id(reviewedRealm.id)
    .title(reviewedRealm.note.noteTopology.title)
    .content(
      `---
${property}:
  - "${token}"
---

## Body
`
    )
    .inNotebook(
      reviewedRealm.notebookRealm.notebook.id,
      reviewedRealm.notebookRealm.notebook.name
    )
    .please()
}
