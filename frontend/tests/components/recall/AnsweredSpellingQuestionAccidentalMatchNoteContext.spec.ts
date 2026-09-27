import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, it, expect } from "vitest"
import {
  accidentalMatchWithOneMatchedNote,
  mountAnsweredSpellingQuestion,
} from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion accidental match note context", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("links the matched note while the note context shows the reviewed note", async () => {
    const { answeredQuestion, reviewedRealm } =
      accidentalMatchWithOneMatchedNote()
    const showNote = mockSdkService(NoteController, "showNote", reviewedRealm)

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    const alert = wrapper.find('[data-testid="accidental-match-alert"]')
    expect(
      alert
        .find('[data-testid="accidental-match-answer-link"]')
        .attributes("to")
    ).toMatch(/10/)
    expect(showNote).toHaveBeenCalledWith({ path: { note: reviewedRealm.id } })
    expect(showNote).not.toHaveBeenCalledWith({ path: { note: 10 } })
    expect(
      wrapper
        .find(".note-under-question")
        .find('[aria-label="Note context"]')
        .text()
    ).toContain("Reviewed Note")
    expect(
      wrapper.find('[data-testid="resolve-accidental-match"]').exists()
    ).toBe(true)
  })

  it("marks the focused property in the reviewed note context for a property tracker", async () => {
    const reviewedRealm = makeMe.aNoteRealm
      .title("Reviewed Note")
      .content("---\ncolor: red\nsize: big\n---\nReviewed body.")
      .please()
    const matched = makeMe.aNoteRealm.id(10).title("Matched A").please()
    mockSdkService(NoteController, "showNote", reviewedRealm)
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(reviewedRealm.note)
      .withPropertyKey("color")
      .accidentalMatch("matched a", [matched.note.noteTopology])
      .please()

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    expect(
      wrapper.find('[data-testid="accidental-match-alert"]').text()
    ).toContain("matched a")
    const noteContext = wrapper
      .find(".note-under-question")
      .find('[aria-label="Note context"]')
    expect(noteContext.text()).toContain("Reviewed Note")
    expect(
      noteContext
        .find('[data-property-key="color"]')
        .attributes("data-property-focused")
    ).toBe("true")
  })
})
