import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { beforeEach, describe, it, expect } from "vitest"
import { mountAnsweredSpellingQuestion } from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion plain wrong", () => {
  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

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
    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
  })
})
