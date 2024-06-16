import { QuizQuestionAndAnswer } from "@/generated/backend";
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid";

describe("isMCQWithAnswerValid", () => {
  it("should return true when the MCQWithAnswer is valid", () => {
    const validMCQWithAnswer: QuizQuestionAndAnswer = {
      id: 1,
      correctAnswerIndex: 0,
      quizQuestion: {
        id: 1,
        multipleChoicesQuestion: {
          stem: "Valid question",
          choices: ["Valid choice 1", "Valid choice 2"],
        },
      },
    };

    expect(isMCQWithAnswerValid(validMCQWithAnswer)).toBe(true);
  });

  it("should return false when the MCQWithAnswer is invalid", () => {
    const invalidMCQWithAnswer: QuizQuestionAndAnswer = {
      id: 1,
      correctAnswerIndex: -1,
      quizQuestion: {
        id: 1,
        multipleChoicesQuestion: {
          stem: "",
          choices: ["", ""],
        },
      },
    };

    expect(isMCQWithAnswerValid(invalidMCQWithAnswer)).toBe(false);
  });
  it("should return false when the second choice is empty", () => {
    const mcqWithAnswer: QuizQuestionAndAnswer = {
      id: 1,
      correctAnswerIndex: 0,
      quizQuestion: {
        id: 1,
        multipleChoicesQuestion: {
          stem: "Valid question",
          choices: ["Valid choice 1", "", "Valid choice 3"],
        },
      },
    };

    expect(isMCQWithAnswerValid(mcqWithAnswer)).toBe(false);
  });
});
