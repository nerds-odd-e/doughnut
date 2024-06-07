import { MCQWithAnswer } from "../../frontend/src/generated/backend/models/MCQWithAnswer"
import mock_services from "./mock_services"

export const questionGenerationService = () => ({
  resetAndStubAskingMCQ: (record: Record<string, string>) => {
    const mcqWithAnswer: MCQWithAnswer = {
      stem: record["Question Stem"],
      correctChoiceIndex: 0,
      choices: [
        record["Correct Choice"]!,
        record["Incorrect Choice 1"]!,
        record["Incorrect Choice 2"]!,
      ],
    }
    const reply = JSON.stringify(mcqWithAnswer)
    cy.then(async () => {
      await mock_services.openAi().restartImposter()
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({ role: "user", content: "Memory Assistant" })
        .stubQuestionGeneration(reply)
    })
  },
  stubEvaluationQuestion: (record: Record<string, boolean | string | number[]>) => {
    cy.then(async () => {
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({ role: "user", content: ".*critically check.*" })
        .stubQuestionEvaluation(JSON.stringify(record))
    })
  },
})
