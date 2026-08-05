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

  it("shows warning try-again alert and emits retry", async () => {
    const answeredQuestion = makeMe.anAnsweredQuestion
      .overlap("Shared Title")
      .please()

    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    const alert = wrapper.find('[data-testid="overlap-try-again-alert"]')
    expect(alert.classes()).toContain("daisy-alert-warning")
    expect(alert.text()).toContain(
      "Correct, but we're looking for another answer — try again."
    )

    await wrapper.find('[data-testid="overlap-try-again"]').trigger("click")
    expect(wrapper.emitted("retry")).toHaveLength(1)
  })

  it("hides matched-notes section even when matchedNotes leak on OVERLAP", async () => {
    const leakedPartner = makeMe.aNote.id(99).title("Leaked Partner").please()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .overlap("Shared Title")
      .withMatchedNotes([leakedPartner.noteTopology])
      .please()

    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
    expect(wrapper.text()).not.toContain("Link to this note")
  })
})
