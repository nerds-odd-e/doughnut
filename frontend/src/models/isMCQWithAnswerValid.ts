import { QuestionAndAnswer } from "@/generated/backend"

function isMCQWithAnswerValid(mcqWithAnswer: QuestionAndAnswer): boolean {
  const { stem, choices } = mcqWithAnswer.multipleChoicesQuestion
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
