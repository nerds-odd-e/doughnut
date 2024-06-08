import { MCQWithAnswer } from "@/generated/backend";

function isMCQWithAnswerValid(mcqWithAnswer: MCQWithAnswer): boolean {
  const { stem, choices } = mcqWithAnswer.multipleChoicesQuestion;
  const { correctChoiceIndex } = mcqWithAnswer;

  const isStemNotEmpty = !!stem?.trim();
  const allChoicesFilled = choices.every((option) => option.trim());
  const isValidChoiceIndex =
    correctChoiceIndex >= 0 && correctChoiceIndex < choices.length;

  return isStemNotEmpty && allChoicesFilled && isValidChoiceIndex;
}

export default isMCQWithAnswerValid;
