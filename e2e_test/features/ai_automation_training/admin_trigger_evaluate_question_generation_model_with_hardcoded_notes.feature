@usingMockedOpenAiService
Feature: Trigger evaluate question generation model with hardcoded notes as admin

  As an admin,
  I want to trigger evaluate question generation model with hardcoded notes directly
  through 1 button click without accessing OpenAI website

  Background:
    Given there is a question generation model "question_generation_model" on my OpenAI account
    And there is a evaluation model "evaluation_model" on my OpenAI account
    And I am logged in as an admin and click AdminDashboard and go to tab "Evaluate Question Model"

  Scenario: Trigger evaluate question generation model with hardcoded notes as admin
      Given there will be evaluation result returned from evaluation model
      When I evaluate a question model
      Then I can see evaluation score of the question model
