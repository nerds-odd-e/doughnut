import { QuizQuestionAndAnswer } from "@/generated/backend";

function isRefineMCQWithAnswerValid(
  mcqWithAnswer: QuizQuestionAndAnswer,
): boolean {
  const { stem, choices } = mcqWithAnswer.quizQuestion.multipleChoicesQuestion;

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
