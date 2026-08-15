import type { Mcq } from "@generated/doughnut-backend-api"

function isMCQWithAnswerValid(mcq: Mcq): boolean {
  const { questionStem: stem, responseChoices: choices } =
    mcq.multipleChoicesQuestion
  const { correctAnswerIndex } = mcq

  const isStemNotEmpty = !!stem?.trim()
  const allChoicesFilled = choices.every((option) => option.trim())
  const isValidChoiceIndex =
    correctAnswerIndex !== undefined &&
    correctAnswerIndex >= 0 &&
    correctAnswerIndex < choices.length

  return isStemNotEmpty && allChoicesFilled && isValidChoiceIndex
}

export default isMCQWithAnswerValid
