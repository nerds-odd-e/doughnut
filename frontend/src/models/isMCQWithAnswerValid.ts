import { QuizQuestionAndAnswer } from "@/generated/backend"

function isMCQWithAnswerValid(mcqWithAnswer: QuizQuestionAndAnswer): boolean {
  const { stem, choices } = mcqWithAnswer.quizQuestion.multipleChoicesQuestion
  const { correctAnswerIndex } = mcqWithAnswer

  const isStemNotEmpty = !!stem?.trim()
  const allChoicesFilled = choices.every((option) => option.trim())
  const isValidChoiceIndex =
    correctAnswerIndex !== undefined &&
    correctAnswerIndex >= 0 &&
    correctAnswerIndex < choices.length

  return isStemNotEmpty && allChoicesFilled && isValidChoiceIndex
}

export default isMCQWithAnswerValid
