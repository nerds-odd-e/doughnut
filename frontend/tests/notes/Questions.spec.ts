import Questions from "@/components/notes/Questions.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

const note = makeMe.aNoteRealm.please()
const createWrapper = async () => {
  const wrapper = helper
    .component(Questions)
    .withProps({
      note: note.note,
    })
    .render()
  await flushPromises()
  return wrapper
}
const stubQuestion = makeMe.aQuizQuestionAndAnswer.please()

describe("Questions", () => {
  it("removes questions", async () => {
    helper.managedApi.restQuizQuestionController.getAllQuestionByNote = vi
      .fn()
      .mockResolvedValue([stubQuestion])
    helper.managedApi.restQuizQuestionController.removeQuestionManually =
      vi.fn()
    const wrapper = await createWrapper()
    helper.managedApi.restQuizQuestionController.getAllQuestionByNote = vi
      .fn()
      .mockResolvedValue([])
    const removeButton = await wrapper.findByText("Remove")
    removeButton.click()
    await flushPromises()
    expect(
      helper.managedApi.restQuizQuestionController.removeQuestionManually
    ).toBeCalledWith(stubQuestion.id)
    await expect(wrapper.findByText("Remove")).rejects.toThrowError()
  })
})
