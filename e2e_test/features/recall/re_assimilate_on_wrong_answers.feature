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
    # Day 1: Assimilate the note
    Given It's day 1, 8 hour
    Given OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And I assimilate the note "sedition"

    # Day 2: First wrong answer
    When I am recalling my note on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect

    # Day 3: Second wrong answer
    When I am recalling my note on day 3
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect

    # Day 4: Third wrong answer
    When I am recalling my note on day 4
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect

    # Day 5: Fourth wrong answer
    When I am recalling my note on day 5
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect

    # Day 6: Fifth wrong answer - should trigger re-assimilation
    When I am recalling my note on day 6
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect

    # After 5 wrong answers, note should be back in assimilate queue
    # Verify by navigating to day 7 and checking counts
    When I am recalling my note on day 7
    Then I should see that I have no notes to recall today
    And I should see that I have 1 new notes to assimilate
