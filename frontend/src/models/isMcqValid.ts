import type { Mcq } from "@generated/donut-backend-api"

function isMcqValid(mcq: Mcq): boolean {
  const {
    questionStem: stem,
    responseChoices: choices,
    correctAnswerIndex,
  } = mcq

  const isStemNotEmpty = !!stem?.trim()
  const allChoicesFilled = choices.every((option) => option.trim())
  const isValidChoiceIndex =
    correctAnswerIndex !== undefined &&
    correctAnswerIndex >= 0 &&
    correctAnswerIndex < choices.length

  return isStemNotEmpty && allChoicesFilled && isValidChoiceIndex
}

export default isMcqValid
