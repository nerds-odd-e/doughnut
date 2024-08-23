import { Note } from "@/generated/backend"
import makeMe from "../../fixtures/makeMe"
import helper from "../../helpers"
import Questions from "@/components/notes/Questions.vue"
import { beforeEach, expect } from "vitest"
import { flushPromises } from "@vue/test-utils"

describe("Questions.spec", () => {
  const note: Note = {
    ...makeMe.aNote.please(),
  }
  let wrapper
  const deleteCall = vi.fn().mockResolvedValue({})

  beforeEach(() => {
    const questions = [
      makeMe.aQuizQuestionAndAnswer.please(),
      makeMe.aQuizQuestionAndAnswer.please(),
      makeMe.aQuizQuestionAndAnswer.please(),
    ]
    helper.managedApi.restQuizQuestionController.getAllQuestionByNote = vi
      .fn()
      .mockResolvedValue(questions)
    helper.managedApi.restQuizQuestionController.removeQuestion = deleteCall
    wrapper = helper.component(Questions).withProps({ note }).mount()
  })

  it("Renders list of questions", async () => {
    expect(
      (wrapper.find(".question-table").element as HTMLTableElement).rows.length
    ).toBe(4)
  })

  it("Deletes the question", async () => {
    // wrapper.find(".question-table > tr > button").trigger("click")
    expect(wrapper.findAll(".question-table tbody tr").length).toBe(3)
    wrapper.findAll(".question-table tbody tr td button")[0].trigger("click")
    await flushPromises()
    expect(wrapper.findAll(".question-table tbody tr").length).toBe(2)
  })
})
