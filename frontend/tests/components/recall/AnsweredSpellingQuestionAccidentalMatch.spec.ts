import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
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

  it("shows compact accidental-match result with Resolve CTA and no stacked matches", async () => {
    const { answeredQuestion } = accidentalMatchWithTwoMatchedNotes()
    const reviewedId = answeredQuestion.recalledNote.noteTopology.id

    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
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

  it("opens resolve dialog listing matched note titles only", async () => {
    const { answeredQuestion } = accidentalMatchWithTwoMatchedNotes()
    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      withRouter: true,
    })
    await flushPromises()

    await wrapper
      .find('[data-testid="resolve-accidental-match"]')
      .trigger("click")
    await flushPromises()

    const dialog = document.body.querySelector(
      '[data-testid="accidental-match-resolve-dialog"]'
    )
    expect(dialog).toBeTruthy()
    expect(
      document.body.querySelector('[data-testid="resolve-match-row-10"]')
        ?.textContent
    ).toContain("Matched A")
    expect(
      document.body.querySelector('[data-testid="resolve-match-row-20"]')
        ?.textContent
    ).toContain("Matched B")
  })
})
