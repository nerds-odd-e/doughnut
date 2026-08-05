import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { beforeEach, describe, it, expect } from "vitest"
import {
  accidentalMatchWithTwoMatchedNotes,
  mountAnsweredSpellingQuestion,
} from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion accidental match", () => {
  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  it("shows distinct alert copy and a matched notes section with NoteShows", async () => {
    const matchedA = makeMe.aNote.id(10).title("Matched A").please()
    const matchedB = makeMe.aNote.id(20).title("Matched B").please()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .accidentalMatch("matched a", [
        matchedA.noteTopology,
        matchedB.noteTopology,
      ])
      .please()

    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    expect(wrapper.text()).toContain(
      "Your answer `matched a` names another note — not correct for this review."
    )
    const matchedSection = wrapper.find('[data-testid="matched-notes-section"]')
    expect(matchedSection.exists()).toBe(true)
    const matchedShows = matchedSection.findAllComponents({ name: "NoteShow" })
    expect(matchedShows.map((show) => show.props("noteId"))).toEqual([10, 20])
    expect(
      matchedShows.every((show) => show.props("expandChildren") === false)
    ).toBe(true)
  })

  it("omits matched notes section when matchedNotes is empty", async () => {
    const answeredQuestion = makeMe.anAnsweredQuestion
      .accidentalMatch("ghost", [])
      .please()

    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
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
      wrapper.findAll('[data-testid^="link-to-matched-note-"]')
    ).toHaveLength(0)
  })
})
