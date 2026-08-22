@usingMockedOpenAiService
Feature: Frequent failure warning after too many wrong answers
  As a learner, when I keep answering a note wrong,
  I should see a warning about how often I've failed.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | sedition | Sedition means incite violence |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   | to stay silent     |
    And OpenAI evaluates the question as legitimate
    And the note "sedition" was assimilated on day 1

  Scenario: Note-level frequent failure warning after threshold exceeded
    When I make 5 wrong answers over 5 days since day 2, answering "to sleep" to "What is the meaning of sedition?"
    Then I should see a frequent failure warning for the note with 5 wrong answers in 14 days
