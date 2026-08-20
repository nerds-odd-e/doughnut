import {
  NoteController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { closeButtonEl } from "@tests/commons/modalTestSupport"
import makeMe from "doughnut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, it, expect } from "vitest"
import {
  accidentalMatchWithTwoMatchedNotes,
  mountAnsweredSpellingQuestion,
  openResolveAccidentalMatch,
} from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion accidental match", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("shows compact accidental-match result with Resolve CTA and no stacked matches", async () => {
    const { answeredQuestion } = accidentalMatchWithTwoMatchedNotes()
    const reviewedId = answeredQuestion.recalledNote.noteTopology.id

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    expect(wrapper.text()).toContain(
      "Your answer `matched a` names another note — not correct for this review."
    )
    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
    expect(
      wrapper
        .findAll('[data-testid="note-show-stub"]')
        .map((show) => Number(show.attributes("data-note-id")))
    ).toEqual([reviewedId])

    const resolveCta = wrapper.find('[data-testid="resolve-accidental-match"]')
    expect(resolveCta.exists()).toBe(true)
    expect(resolveCta.attributes("title")).toBe("Resolve accidental match")
    expect(resolveCta.attributes("aria-label")).toBe("Resolve accidental match")
  })

  it("opens resolve dialog with clickable titles and notebook path identity", async () => {
    const { answeredQuestion, matchedA, matchedB } =
      accidentalMatchWithTwoMatchedNotes({
        notebookNames: ["Notebook Alpha", "Notebook Beta"],
      })
    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      seedRealms: [matchedA, matchedB],
    })
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    const dialog = document.body.querySelector(
      '[data-testid="accidental-match-resolve-dialog"]'
    )
    expect(dialog).toBeTruthy()

    const row10 = document.body.querySelector(
      '[data-testid="resolve-match-row-10"]'
    )
    expect(row10?.textContent).toContain("Matched A")
    // RenderingHelper stubs router-link with href="#"; navigation target is on `to`
    expect(row10?.querySelector("a.router-link")?.getAttribute("to")).toMatch(
      /10/
    )
    expect(
      document.body.querySelector('[data-testid="resolve-match-path-10"]')
        ?.textContent
    ).toContain("Notebook Alpha")

    // Delta only: second row path identity differs when seeded distinctly
    expect(
      document.body.querySelector('[data-testid="resolve-match-path-20"]')
        ?.textContent
    ).toContain("Notebook Beta")

    expect(dialog?.querySelector('[data-testid="note-show-stub"]')).toBeNull()
  })

  it("omits Resolve CTA when matchedNotes is empty", async () => {
    const answeredQuestion = makeMe.anAnsweredQuestion
      .accidentalMatch("ghost", [])
      .please()

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    expect(
      wrapper.find('[data-testid="resolve-accidental-match"]').exists()
    ).toBe(false)
  })

  it("dismisses resolve dialog via close button and stays on accidental-match result", async () => {
    const { answeredQuestion } = accidentalMatchWithTwoMatchedNotes()
    wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    expect(
      document.body.querySelector(
        '[data-testid="accidental-match-resolve-dialog"]'
      )
    ).toBeTruthy()

    closeButtonEl()!.click()
    await flushPromises()

    expect(
      document.body.querySelector(
        '[data-testid="accidental-match-resolve-dialog"]'
      )
    ).toBeNull()
    expect(
      wrapper.find('[data-testid="accidental-match-alert"]').exists()
    ).toBe(true)
  })

  it("builds a link as a same-Modal step and returns to the match list after success", async () => {
    const reviewedRealm = makeMe.aNoteRealm.title("Reviewed Note").please()
    const matched = makeMe.aNoteRealm.id(10).title("Matched A").please()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(reviewedRealm.note)
      .accidentalMatch("matched a", [matched.note.noteTopology])
      .please()
    mockSdkService(TextContentController, "updateNoteContent", reviewedRealm)

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      currentUser: makeMe.aUser.please(),
      seedRealms: [reviewedRealm, matched],
    })
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    ;(
      document.body.querySelector(
        '[data-testid="wiki-link-or-relationship-to-matched-note-10"]'
      ) as HTMLElement
    ).click()
    await flushPromises()

    expect(
      document.body.querySelector(
        '[data-testid="accidental-match-resolve-dialog"]'
      )
    ).toBeNull()
    expect(document.body.textContent).toContain("Target:")

    ;(document.body.querySelector(".daisy-btn-accent") as HTMLElement).click()
    await flushPromises()

    expect(
      document.body.querySelector(
        '[data-testid="accidental-match-resolve-dialog"]'
      )
    ).toBeTruthy()
    expect(
      wrapper.find('[data-testid="accidental-match-alert"]').exists()
    ).toBe(true)
  })

  it("omits mutating CTAs when reviewed notebook is readonly", async () => {
    const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
      accidentalMatchWithTwoMatchedNotes({ reviewedReadonly: true })

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      currentUser: makeMe.aUser.please(),
      seedRealms: [reviewedRealm, matchedA, matchedB],
    })
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    expect(
      document.body.querySelectorAll(
        '[data-testid^="wiki-link-or-relationship-to-matched-note-"]'
      )
    ).toHaveLength(0)
    expect(
      document.body.querySelectorAll('[data-testid^="add-as-overlapped-note-"]')
    ).toHaveLength(0)
  })

  it("omits mutating CTAs when note realms are not loaded", async () => {
    mockSdkServiceWithImplementation(
      NoteController,
      "showNote",
      () =>
        new Promise(() => {
          // never settles — realms stay unloaded for AMR-07 gate
        })
    )
    const { answeredQuestion } = accidentalMatchWithTwoMatchedNotes()

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      currentUser: makeMe.aUser.please(),
    })
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    expect(
      document.body.querySelectorAll(
        '[data-testid^="wiki-link-or-relationship-to-matched-note-"]'
      )
    ).toHaveLength(0)
    expect(
      document.body.querySelectorAll('[data-testid^="add-as-overlapped-note-"]')
    ).toHaveLength(0)
  })
})
