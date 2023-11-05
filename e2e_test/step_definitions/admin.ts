/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check
import { DataTable, Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"
import start, { mock_services } from "start"

Given("my question should not be included in the admin's fine-tuning data", () => {
  start
    .loginAsAdminAndGoToAdminDashboard()
    .suggestedQuestionsForFineTuning()
    .downloadAIQuestionTrainingData()
    .expectNumberOfRecords(0)
})

When("I upload the feedbacks", () => {
  start
    .loginAsAdminAndGoToAdminDashboard()
    .suggestedQuestionsForFineTuning()
    .uploadFineTuningTrainingData()
})

Given(
  "the admin modifies the question suggested {string} to:",
  (originalQuestionStem: string, newQuestion: DataTable) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .suggestedQuestionsForFineTuning()
      .updateQuestionSuggestionAndChoice(originalQuestionStem, newQuestion.hashes()[0])
  },
)

Given("an admin duplicates the question {string}", (questionStem: string) => {
  start
    .loginAsAdminAndGoToAdminDashboard()
    .suggestedQuestionsForFineTuning()
    .duplicateNegativeQuestion(questionStem)
})

Given(
  "an admin can retrieve the training data for question generation containing:",
  (question: DataTable) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .suggestedQuestionsForFineTuning()
      .downloadAIQuestionTrainingData()
      .expectExampleQuestions(question.hashes())
  },
)

Given(
  "an admin can retrieve the training data for question generation containing {int} examples",
  (numOfDownload: number) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .suggestedQuestionsForFineTuning()
      .downloadAIQuestionTrainingData()
      .expectNumberOfRecords(numOfDownload)
  },
)

Given(
  "an admin should be able to download the training data for evaluation containing:",
  (trainingExamples: DataTable) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .suggestedQuestionsForFineTuning()
      .downloadFeedbackForEvaluationModel()
      .expectExampleQuestions(trainingExamples.hashes())
  },
)

Given(
  "there should be {int} examples containing {string}",
  (numOfOccurrence: number, expectedString: string) => {
    start
      .assumeAdminDashboardPage()
      .suggestedQuestionsForFineTuning()
      .expectString(numOfOccurrence, expectedString)
  },
)

Given("I log in as an admin and go to {string} in admin dashboard", (tabName: string) => {
  start.loginAsAdminAndGoToAdminDashboard().goToTabInAdminDashboard(tabName)
})

When("I choose {string} for {string} use", (modelName: string, trainingEngine: string) => {
  start.assumeAdminDashboardPage().chooseModelNameInEngine(modelName, trainingEngine)
})

Then(
  "I can choose model {string} from GPT in {string} dropdown list",
  (modelName: string, trainingEngine: string) => {
    start.assumeAdminDashboardPage().assumeAdminCanSeeModelOption(modelName, trainingEngine)
  },
)

Then("I should be using {string} for {string}", (modelName: string, trainingEngine: string) => {
  start
    .assumeAdminDashboardPage()
    .goToTabInAdminDashboard("Failure Reports")
    .goToTabInAdminDashboard("Manage Model")
    .assumeSelectionWithDefaultOption(modelName, trainingEngine)
})

Given("OpenAI response {string} when uploading fine tuning data", (result) => {
  mock_services.openAi().stubOpenAiUploadResponse(result === "success")
})

Given("OpenAi response {string} when trigger fine tuning data", (result) => {
  mock_services.openAi().stubFineTuningStatus(result === "success")
})

When("I trigger fine tuning", () => {
  start.loginAsAdminAndGoToAdminDashboard().suggestedQuestionsForFineTuning().triggerFineTuning()
})

Then("I should see the message {string}", (message: string) => {
  cy.findByText(message).should("exist")
})
