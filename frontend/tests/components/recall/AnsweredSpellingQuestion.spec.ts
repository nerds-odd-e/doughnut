import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, describe, it, expect } from "vitest"
import { mountAnsweredSpellingQuestion } from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion plain wrong", () => {
  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  it("keeps incorrect alert copy and omits matched notes section", async () => {
    const answeredQuestion = makeMe.anAnsweredQuestion
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
    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
  })
})

describe("AnsweredSpellingQuestion correct", () => {
  it("shows the note context for a correct answer", async () => {
    const noteTitle = "Sedition"
    const noteRealm = makeMe.aNoteRealm
      .title(noteTitle)
      .notebookName("Political History")
      .content("Inciting rebellion against authority.")
      .please()
    mockSdkService(NoteController, "showNote", noteRealm)
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(noteRealm.note)
      .spelling()
      .withAnswer({ id: 1, correct: true, spellingAnswer: "Sedition" })
      .please()

    const wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()

    const noteUnderQuestion = wrapper.find(".note-under-question")
    const noteContext = noteUnderQuestion.find('[aria-label="Note context"]')
    expect(noteContext.exists()).toBe(true)
    expect(noteContext.text()).toContain(
      "Inciting rebellion against authority."
    )
    expect(wrapper.text().split(noteTitle).length - 1).toBe(1)
    expect(wrapper.find(".daisy-alert-success").text()).toContain("Correct!")
  })
})
