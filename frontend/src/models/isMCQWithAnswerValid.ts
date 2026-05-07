import type { PredefinedQuestion } from "@generated/doughnut-backend-api"

function isMCQWithAnswerValid(predefinedQuestion: PredefinedQuestion): boolean {
  const { questionStem: stem, responseChoices: choices } =
    predefinedQuestion.multipleChoicesQuestion
  const { correctAnswerIndex } = predefinedQuestion

  const isStemNotEmpty = !!stem?.trim()
  const allChoicesFilled = choices.every((option) => option.trim())
  const isValidChoiceIndex =
    correctAnswerIndex !== undefined &&
    correctAnswerIndex >= 0 &&
    correctAnswerIndex < choices.length

  return isStemNotEmpty && allChoicesFilled && isValidChoiceIndex
}

export default isMCQWithAnswerValid
