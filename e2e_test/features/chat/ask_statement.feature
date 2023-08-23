@usingMockedOpenAiService
Feature: Ask Statement


  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | title        | description                                    |
      | Scuba Diving | The most common certification is Rescue Diver. |
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    And I ask to generate a question for note "Scuba Diving"


  Scenario: The users can conmunicate with AI
    When I ask to OpenAI "What's your name?"
  # Then I can confirm the answer "I'm ChatGPT"


  @ignore
  Scenario: The users can continue to conmunication with AI
    When I input the ask statement "What's your name?"
    And I clicked the ask button
    Then I can confirm the answer include "I'm ChatGPT"
    When I input the ask statement "How many days are there in the year 2023?"
    And I clicked the ask button
    Then I can confirm the answer include "365"