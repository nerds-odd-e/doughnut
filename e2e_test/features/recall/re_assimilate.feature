@usingMockedOpenAiService
Feature: Re-assimilate note after too many wrong answers
  As a learner, when I answer a note wrong too many times,
  the note should return to assimilate state for re-learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |

  Scenario: Note returns to assimilation after 5 wrong answers
    Given the note "sedition" was assimilated on day 1
    When I make 5 wrong answers over 5 days since day 2, answering "to sleep" to "What is the meaning of sedition?"
    And I confirm re-assimilation
    Then I should see 1 due for assimilation
