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

  Scenario: AI will generate a question
    Then I should see a question on current page
      | question                                            | option_a     | option_b   | option_c         |
      | What is the most common scuba diving certification? | Rescue Diver | Divemaster | Open Water Diver |

  Scenario Outline: I should see feedback on selecting options
    Then I should be asked "What is the most common scuba diving certification?"
    And the option "<option>" should be <expectedResult>
    Examples:
      | option       | expectedResult |
      | Rescue Diver | correct        |
      | Divemaster   | wrong          |

  Scenario: I should see a new question when I click 'Ask again'
    Given OpenAI by default returns this question from now:
      | question                                             | option_a                 | option_b                   | option_c                       |
      | What is the least common scuba diving certification? | Divemaster certification | Rescue Diver certification | Open Water Diver certification |
    When I click "Ask again"
    Then I should see a question on current page
      | question                                             | option_a                 | option_b                   | option_c                       |
      | What is the least common scuba diving certification? | Divemaster certification | Rescue Diver certification | Open Water Diver certification |

  Scenario Outline: I should see my previous answer unselected when a new question is asked
    When I select the <CorrectOrWrong> option <Option>
    And I click "Ask again"
    Then none of the options should be selected
    Examples:
      | Option | CorrectOrWrong |
      | a      | correct        |
      | b      | wrong          |
