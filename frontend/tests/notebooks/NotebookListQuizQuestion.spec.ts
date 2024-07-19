import NotebookListQuizQuestion from "@/components/notebook/NotebookListQuizQuestion.vue"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"
import { flushPromises } from "@vue/test-utils"
import { screen } from "@testing-library/vue"
import { Note, QuizQuestionAndAnswer } from "@/generated/backend"

const noteBook = makeMe.aNotebook.please()
const createWrapper = async () => {
  return helper
    .component(NotebookListQuizQuestion)
    .withProps({
      notebookId: noteBook.id,
    })
    .render()
}

describe("All questions show", () => {
  let note: Note
  let quiz: QuizQuestionAndAnswer
  beforeEach(() => {
    note = makeMe.aNote.please()
    quiz = makeMe.aQuizQuestionAndAnswer.please()
    note.quizQuestionAndAnswers = [quiz]
    helper.managedApi.restNotebookController.getNotes = vi
      .fn()
      .mockResolvedValue([note])
  })

  it("should show note question", async () => {
    await createWrapper()
    await flushPromises()
    const quizName = quiz.quizQuestion.multipleChoicesQuestion.stem ?? ""
    const noteName = note.noteTopic.topicConstructor
    await screen.findByText(quizName)
    await screen.findByText(noteName)
  })
})
