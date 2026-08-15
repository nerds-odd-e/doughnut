import type { Mcq } from "@generated/doughnut-backend-api"
import isMcqValid from "@/models/isMcqValid"
import makeMe from "doughnut-test-fixtures/makeMe"

describe("isMcqValid", () => {
  it("should return true when the Mcq is valid", () => {
    const validMcq: Mcq = makeMe.anMcq
      .withQuestionStem("Valid question")
      .withChoices(["Valid choice 1", "Valid choice 2"])
      .please()

    expect(isMcqValid(validMcq)).toBe(true)
  })

  it("should return false when the Mcq is invalid", () => {
    const invalidMcq: Mcq = makeMe.anMcq
      .withQuestionStem("")
      .correctAnswerIndex(-1)
      .withChoices(["", ""])
      .please()

    expect(isMcqValid(invalidMcq)).toBe(false)
  })
  it("should return false when the second choice is empty", () => {
    const mcq: Mcq = makeMe.anMcq
      .withQuestionStem("Valid question")
      .withChoices(["Valid choice 1", "", "Valid choice 3"])
      .please()

    expect(isMcqValid(mcq)).toBe(false)
  })
})
