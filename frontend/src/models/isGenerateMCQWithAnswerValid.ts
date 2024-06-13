import { MCQWithAnswer } from "@/generated/backend";

function isGenerateMCQWithAnswerValid(mcqWithAnswer: MCQWithAnswer): boolean {
  const { stem } = mcqWithAnswer.multipleChoicesQuestion;

  return !!stem?.trim();
}

export default isGenerateMCQWithAnswerValid;
