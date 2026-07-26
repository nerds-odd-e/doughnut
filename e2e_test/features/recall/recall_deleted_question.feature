Feature: Recall after predefined question deleted
  As a learner, I want a clear message when a recall question was deleted
  so I am not stuck on an empty quiz.

  Background:
    Given I am logged in as an existing user

  @usingMockedOpenAiService
  Scenario: Deleted question shows warning during recall
    Given I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | sedition | Sedition means incite violence |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And OpenAI evaluates the question as legitimate
    And the note "sedition" was assimilated on day 1
    When I am recalling my note on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I delete the question "What is the meaning of sedition?" from the note "sedition"
    And I am recalling my note on day 2
    Then I should see that the question was deleted and cannot be reviewed
    When I choose that I need more recall
    Then I should see that I have finished all recalls for this half a day
