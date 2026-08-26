import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, it, expect } from "vitest"
import { mountAnsweredSpellingQuestion } from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion overlap try-again", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("shows warning try-again alert and emits retry", async () => {
    const answeredQuestion = makeMe.anAnsweredQuestion
      .overlap("Shared Title")
      .please()

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    const alert = wrapper.find('[data-testid="overlap-try-again-alert"]')
    expect(alert.classes()).toContain("daisy-alert-warning")
    expect(alert.text()).toContain(
      "Correct, but we're looking for another answer — try again."
    )

    await wrapper.find('[data-testid="overlap-try-again"]').trigger("click")
    expect(wrapper.emitted("retry")).toHaveLength(1)
  })

  it("omits Resolve CTA even when matchedNotes leak on OVERLAP", async () => {
    const leakedPartner = makeMe.aNote.id(99).title("Leaked Partner").please()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .overlap("Shared Title")
      .withMatchedNotes([leakedPartner.noteTopology])
      .please()

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    expect(
      wrapper.find('[data-testid="resolve-accidental-match"]').exists()
    ).toBe(false)
    expect(
      document.body.querySelector(
        '[data-testid="accidental-match-resolve-dialog"]'
      )
    ).toBeNull()
  })
})
