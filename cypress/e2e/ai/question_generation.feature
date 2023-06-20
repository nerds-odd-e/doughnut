@usingMockedOpenAiService
Feature: Question generation

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title        | description                                   |
      | Scuba Diving | The most common certification is Rescue Diver.|
    And OpenAI by default returns this question from now:
      | question                                            | option_a     | option_b   | option_c         |
      | What is the most common scuba diving certification? | Rescue Diver | Divemaster | Open Water Diver |
    When I ask to generate a question for note "Scuba Diving"

  Scenario Outline: testing myself with generated question for a note
    Then I should be asked "What is the most common scuba diving certification?"
    And the option "<option>" should be <expectedResult>
    Examples:
      | option       | expectedResult |
      | Rescue Diver | correct        |
      | Divemaster   | wrong          |

  Scenario: I should see a new question when I click 'Ask again'
    Given OpenAI by default returns this question from now:
      | question                | option_a | option_b | option_c  |
      | How often scuba diving? | daily    | weekly   | never     |
    When I click "Ask again"
    Then I should be asked "How often scuba diving?"
