@usingMockedOpenAiService
Feature: Trigger fine-tuning directly as admin

  As an admin,
  I want to trigger fine-tuning directly through 1 button click without accessing OpenAI website

  Background:
    Given there is a fine-tuning file "question_gerenation_examples" on my OpenAI account
    And I am logged in as an admin and click AdminDashboard and go to tab "Fine Tuning Data"

  Scenario Outline:
      Given the finetuning for the file "question_generation_examples" will be <API response>
      When I choose the file "question generation examples" based on GPT3.5 model
      Then I will see success message "<expected message>"

    Examples:
    | API response | expected message |
    | successful   | successful       |
#    | failed       | failed message   |
