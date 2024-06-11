import { MCQWithAnswer } from "@/generated/backend";

function isRefineMCQWithAnswerValid(mcqWithAnswer: MCQWithAnswer): boolean {
  const { stem, choices } = mcqWithAnswer.multipleChoicesQuestion;

  const isStemNotEmpty = !!stem?.trim();
  let isChoiceNotEmpty = false;

  choices.forEach((option) => {
    if (option.trim()) {
      isChoiceNotEmpty = true;
    }
  });

  return isStemNotEmpty || isChoiceNotEmpty;
}

export default isRefineMCQWithAnswerValid;
