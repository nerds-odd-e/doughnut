@usingMockedOpenAiService
Feature: Generate Training Data from marked questions
  As a developer, I want to extract marked good questions
  So that I can provide in a format for OpenAI training data format for model trainer

  Scenario: 0 marked good questions
    Given that there are no questions marked good at all
    When I attempt to export
    Then I should return an empty JSONL file

  @ignore
  Scenario: 1 or more good questions
    # Given There is a marked question
    Given I've logged in as "developer"
    And there are some notes for the current user:
      | topic        | details                                        |
      | Scuba Diving | The most common certification is Rescue Diver. |
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    When I attempt to export
    Then a file with training data is produced
