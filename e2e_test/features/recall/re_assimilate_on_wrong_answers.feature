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

  Scenario: Note returns to assimilate after 5 wrong answers within 14 days
    Given It's day 1, 8 hour
    Given OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And I assimilate the note "sedition"

    # 5 consecutive wrong answers for 5 days since day 2
    When I make a wrong answer on day 2, answering "to sleep" to "What is the meaning of sedition?"
    And I make a wrong answer on day 3, answering "to sleep" to "What is the meaning of sedition?"
    And I make a wrong answer on day 4, answering "to sleep" to "What is the meaning of sedition?"
    And I make a wrong answer on day 5, answering "to sleep" to "What is the meaning of sedition?"
    And I make a wrong answer on day 6, answering "to sleep" to "What is the meaning of sedition?"
    Then I should see a re-assimilate confirmation dialog
    When I confirm to re-assimilate the note

    # After confirming re-assimilation, note should be back in assimilate queue
    # Verify by navigating to day 7 and checking counts
    When I am recalling my note on day 7
    Then I should see that I have no notes to recall today
    And I should see that I have 1 new notes to assimilate
    And I should see the assimilation counter increased by 1 in the sidebar
