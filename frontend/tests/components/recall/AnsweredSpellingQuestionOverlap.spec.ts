import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { beforeEach, describe, it, expect } from "vitest"
import { mountAnsweredSpellingQuestion } from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion overlap try-again", () => {
  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  it("shows warning try-again alert without matched notes and emits retry", async () => {
    const reviewed = makeMe.aNote.title("Reviewed Note").please()
    const answeredQuestion: AnsweredQuestion = {
      ...makeMe.anAnsweredQuestion
        .withNote(reviewed)
        .spelling()
        .answerCorrect(false)
        .withAnswer({
          id: 1,
          correct: false,
          spellingAnswer: "Shared Title",
          outcome: "OVERLAP",
        })
        .please(),
    }

    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    const alert = wrapper.find('[data-testid="overlap-try-again-alert"]')
    expect(alert.exists()).toBe(true)
    expect(alert.classes()).toContain("daisy-alert-warning")
    expect(alert.text()).toContain(
      "Correct, but we're looking for another answer — try again."
    )
    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
    expect(
      wrapper.find('[data-testid="accidental-match-alert"]').exists()
    ).toBe(false)

    const tryAgain = wrapper.find('[data-testid="overlap-try-again"]')
    expect(tryAgain.exists()).toBe(true)
    expect(tryAgain.text()).toContain("Try again")
    await tryAgain.trigger("click")
    expect(wrapper.emitted("retry")).toHaveLength(1)
  })

  it("keeps matched-notes section and offer-link CTAs absent when matchedNotes leak on OVERLAP", async () => {
    const reviewed = makeMe.aNote.title("Reviewed Note").please()
    const leakedPartner = makeMe.aNote.title("Leaked Partner").please()
    leakedPartner.id = 99
    leakedPartner.noteTopology.id = 99

    const answeredQuestion: AnsweredQuestion = {
      ...makeMe.anAnsweredQuestion
        .withNote(reviewed)
        .spelling()
        .answerCorrect(false)
        .withAnswer({
          id: 1,
          correct: false,
          spellingAnswer: "Shared Title",
          outcome: "OVERLAP",
        })
        .withMatchedNotes([leakedPartner.noteTopology])
        .please(),
    }

    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    expect(
      wrapper.find('[data-testid="overlap-try-again-alert"]').exists()
    ).toBe(true)
    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
    expect(wrapper.text()).not.toContain("Link to this note")
    expect(wrapper.text()).not.toContain("Matched note(s)")
  })
})
