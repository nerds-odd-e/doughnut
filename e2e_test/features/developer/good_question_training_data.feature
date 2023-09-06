@usingMockedOpenAiService
Feature: Generate Training Data from marked questions
  As a developer, I want to extract marked good questions
  So that I can provide in a format for OpenAI training data format for model trainer

  Scenario: 0 marked good questions
    Given that there are no questions marked good at all
    When I attempt to export
    Then I should return an empty JSONL file


  Scenario: 1 or more good questions
    # Given There is a marked question
    Given I've logged in as "developer"
    When I attempt to export
    Then a file with training data is produced
