import type { Mcq } from "@generated/doughnut-backend-api"
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import makeMe from "doughnut-test-fixtures/makeMe"

describe("isMCQWithAnswerValid", () => {
  it("should return true when the MCQWithAnswer is valid", () => {
    const validMCQWithAnswer: Mcq = makeMe.anMcq
      .withQuestionStem("Valid question")
      .withChoices(["Valid choice 1", "Valid choice 2"])
      .please()

    expect(isMCQWithAnswerValid(validMCQWithAnswer)).toBe(true)
  })

  it("should return false when the MCQWithAnswer is invalid", () => {
    const invalidMCQWithAnswer: Mcq = makeMe.anMcq
      .withQuestionStem("")
      .correctAnswerIndex(-1)
      .withChoices(["", ""])
      .please()

    expect(isMCQWithAnswerValid(invalidMCQWithAnswer)).toBe(false)
  })
  it("should return false when the second choice is empty", () => {
    const mcqWithAnswer: Mcq = makeMe.anMcq
      .withQuestionStem("Valid question")
      .withChoices(["Valid choice 1", "", "Valid choice 3"])
      .please()

    expect(isMCQWithAnswerValid(mcqWithAnswer)).toBe(false)
  })
})
