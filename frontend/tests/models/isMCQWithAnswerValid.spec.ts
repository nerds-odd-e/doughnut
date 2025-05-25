import type { PredefinedQuestion } from "generated/backend"
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import makeMe from "@tests/fixtures/makeMe"

describe("isMCQWithAnswerValid", () => {
  it("should return true when the MCQWithAnswer is valid", () => {
    const validMCQWithAnswer: PredefinedQuestion = makeMe.aPredefinedQuestion
      .withQuestionStem("Valid question")
      .withChoices(["Valid choice 1", "Valid choice 2"])
      .please()

    expect(isMCQWithAnswerValid(validMCQWithAnswer)).toBe(true)
  })

  it("should return false when the MCQWithAnswer is invalid", () => {
    const invalidMCQWithAnswer: PredefinedQuestion = makeMe.aPredefinedQuestion
      .withQuestionStem("")
      .correctAnswerIndex(-1)
      .withChoices(["", ""])
      .please()

    expect(isMCQWithAnswerValid(invalidMCQWithAnswer)).toBe(false)
  })
  it("should return false when the second choice is empty", () => {
    const mcqWithAnswer: PredefinedQuestion = makeMe.aPredefinedQuestion
      .withQuestionStem("Valid question")
      .withChoices(["Valid choice 1", "", "Valid choice 3"])
      .please()

    expect(isMCQWithAnswerValid(mcqWithAnswer)).toBe(false)
  })
})
