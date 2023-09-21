/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given } from "@badeball/cypress-cucumber-preprocessor"
import pageObjects from "page_objects"

Given(
  "an admin should be able to download the training data with {int} record",
  (count: number) => {
    pageObjects
      .loginAsAdminAndGoToAdminDashboard()
      .downloadAIQuestionTrainingData()
      .expectNumberOfRecords(count)
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
