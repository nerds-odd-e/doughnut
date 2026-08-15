@usingMockedOpenAiService
Feature: Contest AI-generated MCQs
  As a learner, I want to contest an MCQ so I can get a better MCQ for my note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Dive journal" with a note "Scuba Diving"
    And OpenAI generates this as first question:
      | Question Stem  | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | First question | Rescue Diver   | Divemaster         | Open Water Diver   |
    And OpenAI generates this as second question:
      | Question Stem   | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | Second question | Rescue Diver   | Divemaster         | Open Water Diver   |

  Scenario Outline: Internally contested MCQs are replaced before recall
    Given OpenAI evaluates the question as <Legitimate Question>
    And the note "Scuba Diving" was assimilated on day 1
    When I am recalling my note on day 2
    Then I should be asked "<Current Question>"

    Examples:
      | Legitimate Question | Current Question |
      | legitimate          | First question   |
      | not legitimate      | Second question  |

  Scenario: Learner contests an MCQ and gets a replacement
    Given OpenAI evaluates the question as not legitimate
    And the note "Scuba Diving" was assimilated on day 1
    And I am recalling my note on day 2
    When I contest the MCQ
    Then I should be asked "Second question"
