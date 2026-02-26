@usingMockedOpenAiService
Feature: Re-assimilate note after too many wrong answers
  As a learner, when I answer a note wrong too many times,
  the note should return to assimilate state for re-learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |

  Scenario: Note returns to assimilate after 5 wrong answers within 14 days
    Given It's day 1, 8 hour
    And I assimilate the note "sedition"
    When I make 5 consecutive wrong answers for 5 days since day 2, answering "to sleep" to "What is the meaning of sedition?"
    And I confirm to re-assimilate the note
    Then I should see that I have 1 new notes to assimilate
