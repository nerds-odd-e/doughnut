@usingMockedOpenAiService
Feature: Ask AI to generate the question
  As a trainer, I want to create the question by asking the AI to generate the question,
  so that I can use the questions for assessment.

  Background:
    Given I am logged in as an existing user
    And OpenAI now generates this question:
      | Question Stem         | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is scuba diving? | Rescue Diver   | Divemaster         | Open Water Diver   |
    And I have a note with the topic "Countries"
    And I want to create a question for the note "Countries"

  Scenario: Cannot generate the question when question was inputted
    When I fill question the following question for the note "Countries":
      | Question                             | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What do you call a cow with not leg? | Ground beef    | Cowboy             | Oxford             |
    Then The "Generate by AI" button should be disabled

  @ignore
  Scenario: Can generate the question with the all full fill data
    When I generate question by AI
    Then The generated question by AI will show:
      | Question                | Choice 0       | Choice 1   | Choice 2          |
      | What is scuba diving?   | Rescue Diver   | Divemaster | Open Water Diver  |

