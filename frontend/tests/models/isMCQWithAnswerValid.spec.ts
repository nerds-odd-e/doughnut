import { MCQWithAnswer } from "@/generated/backend";
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid";

describe("isMCQWithAnswerValid", () => {
  it("should return true when the MCQWithAnswer is valid", () => {
    const validMCQWithAnswer: MCQWithAnswer = {
      correctChoiceIndex: 0,
      multipleChoicesQuestion: {
        stem: "Valid question",
        choices: ["Valid choice 1", "Valid choice 2"],
      },
    };

    expect(isMCQWithAnswerValid(validMCQWithAnswer)).toBe(true);
  });

  it("should return false when the MCQWithAnswer is invalid", () => {
    const invalidMCQWithAnswer: MCQWithAnswer = {
      correctChoiceIndex: -1,
      multipleChoicesQuestion: {
        stem: "",
        choices: ["", ""],
      },
    };

    expect(isMCQWithAnswerValid(invalidMCQWithAnswer)).toBe(false);
  });
  it("should return false when the second choice is empty", () => {
    const mcqWithAnswer: MCQWithAnswer = {
      correctChoiceIndex: 0,
      multipleChoicesQuestion: {
        stem: "Valid question",
        choices: ["Valid choice 1", "", "Valid choice 3"],
      },
    };

    expect(isMCQWithAnswerValid(mcqWithAnswer)).toBe(false);
  });
});
