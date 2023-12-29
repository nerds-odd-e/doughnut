@usingMockedOpenAiService
Feature: Manage AI assistants

  Background:
    Given I am logged in as an admin
    And OpenAI creates an assistant of ID "new_assistant" for name "note details completion"

  Scenario: Admin creates new assistant
    * I recreate all the assitants and the new assistant ID should be "new_assistant" for "note details completion"



