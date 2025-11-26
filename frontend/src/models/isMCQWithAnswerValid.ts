import type { PredefinedQuestion } from "@generated/backend"

function isMCQWithAnswerValid(predefinedQuestion: PredefinedQuestion): boolean {
  const { f0__stem: stem, f1__choices: choices } =
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
