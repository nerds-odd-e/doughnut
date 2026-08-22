@usingMockedOpenAiService
Feature: Contest AI-generated MCQs
  As a learner, I want to contest an MCQ so I can get a better MCQ for my note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Dive journal" with a note "Scuba Diving"
    And OpenAI generates this question:
      | Question Stem  | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3        |
      | First question | Rescue Diver   | Divemaster         | Open Water Diver   | Advanced Open Water Diver |
    And the note "Scuba Diving" was assimilated on day 1

  Scenario Outline: Internally contested MCQs are replaced before recall
    Given OpenAI evaluates the question as <Legitimate Question>
    And a due recall prompt is ready on day 2
    When I visit recall
    Then I should be asked "<Current Question>"

    Examples:
      | Legitimate Question | Current Question |
      | legitimate          | First question   |
      | not legitimate      | Second question  |

  Scenario: Learner contests an MCQ and gets a replacement
    Given OpenAI will accept the generated question then uphold a contest
    And a due recall prompt is ready on day 2
    When I visit recall
    Then I should be asked "First question"
    When I contest the MCQ
    Then I should be asked "Second question"
