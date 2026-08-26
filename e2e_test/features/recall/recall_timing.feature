@usingMockedOpenAiService
Feature: Recall timing
  As a learner, I want Recall History to show how much time I spent away from
  the tab while answering a question, separately from my thinking time.

  Background:
    Given I am logged in as an existing user
    And OpenAI evaluates the question as legitimate
    And I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | sedition | Sedition means incite violence |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   | to stay silent     |
    And the note "sedition" was assimilated on day 1

  Scenario: Switching away mid-question and back records away time and count
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "What is the meaning of sedition?"
    And I switch away from the tab for 2 seconds
    And I choose answer "to incite violence"
    And I visit the understanding memory tracker for "sedition"
    Then the recall history should show away time and count beside thinking time
