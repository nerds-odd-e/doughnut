import { DataTable } from "@badeball/cypress-cucumber-preprocessor"
/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given } from "@badeball/cypress-cucumber-preprocessor"
import pageObjects from "page_objects"

Given("my question should not be included in the admin's fine-tuning data", () => {
  pageObjects
    .loginAsAdminAndGoToAdminDashboard()
    .suggestedQuestionsForFineTuning()
    .downloadAIQuestionTrainingData()
    .expectNumberOfRecords(0)
})

Given(
  "an admin edit the question and choices {string} with a different question:",
  (originalQuestionStem: string, newQuestion: DataTable) => {
    pageObjects
      .loginAsAdminAndGoToAdminDashboard()
      .suggestedQuestionsForFineTuning()
      .updateQuestionSuggestionAndChoice(originalQuestionStem, newQuestion.hashes()[0])
  },
)

Given(
  "an admin should be able to download the training data containing 1 example containing {string}",
  (questionStem: string) => {
    pageObjects
      .loginAsAdminAndGoToAdminDashboard()
      .suggestedQuestionsForFineTuning()
      .downloadAIQuestionTrainingData()
      .expectNumberOfRecords(1)
      .expectTxtInDownload(questionStem)
  },
)

Given(
  "an admin should be able to download the training data containing 0 examples",
  (questionStem: string) => {
    pageObjects
      .loginAsAdminAndGoToAdminDashboard()
      .suggestedQuestionsForFineTuning()
      .downloadAIQuestionTrainingData()
      .expectNumberOfRecords(0)
  },
)

Given("the admin should see {string} in the suggested questions", (expectedComment: string) => {
  pageObjects
    .loginAsAdminAndGoToAdminDashboard()
    .suggestedQuestionsForFineTuning()
    .expectComment(expectedComment)
})
