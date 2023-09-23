/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given } from "@badeball/cypress-cucumber-preprocessor"
import pageObjects from "page_objects"

Given("my question should not be included in the admin's fine-tuning data", () => {
  pageObjects
    .loginAsAdminAndGoToAdminDashboard()
    .downloadAIQuestionTrainingData()
    .expectNumberOfRecords(0)
})

Given(
  "an admin should be able to download the training data with 1 record containing {string}",
  (questionStem: string) => {
    pageObjects
      .loginAsAdminAndGoToAdminDashboard()
      .downloadAIQuestionTrainingData()
      .expectNumberOfRecords(1)
      .expectTxtInDownload(questionStem)
  },
)

Given(
  "an admin should be able to download the training data with {string} as an improved {string}",
  (suggestion: string, _option: string) => {
    pageObjects
      .loginAsAdminAndGoToAdminDashboard()
      .downloadAIQuestionTrainingData()
      .expectTxtInDownload(suggestion)
  },
)

Given("the admin should see {string} in the downloaded file", (expectedComment: string) => {
  pageObjects
    .loginAsAdminAndGoToAdminDashboard()
    .downloadAIQuestionTrainingData()
    .expectTxtInDownload(expectedComment)
})
