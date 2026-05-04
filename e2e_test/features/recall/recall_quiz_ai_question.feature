Feature: Recall Quiz
  As a learner, I want to use quizzes in my recall to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title   | Details                  | Skip Memory Tracking |
      | English |                          | true                 |
      | sedation | Put to sleep is sedation |                      |
      | medical |                          |                      |

  @usingMockedOpenAiService
  Scenario: AI generated question - incorrect answer
    Given I have a notebook "English practice" with notes:
      | Title    | Details                        |
      | sedition | Sedition means incite violence |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And the note "sedition" was assimilated on day 1
    When I am recalling my note on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect

  @usingMockedOpenAiService
  Scenario: AI generated question - correct answer
    Given I have a notebook "English practice" with notes:
      | Title    | Details                        |
      | sedition | Sedition means incite violence |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And the note "sedition" was assimilated on day 1
    When I am recalling my note on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to incite violence"
    Then I should see that my answer is correct as the last question

  @usingMockedOpenAiService
  Scenario: AI question generation prompt includes outgoing wiki-linked note content
    Given I have a notebook "English practice" with notes:
      | Title    | Details                                                                  |
      | Bahamas  | The Bahamas is an archipelago in the Atlantic.                           |
      | sedition | Sedition means incite violence. Also see [[Bahamas]].                    |
    And OpenAI generates this question only when prompt includes a retrieved wiki-linked note:
      | Question Stem        | Correct Choice  | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the Bahamas? | An archipelago  | A continent        | An act of sedition |
    And the note "sedition" was assimilated on day 1
    When I am recalling my note on day 2
    Then I should be asked "What is the Bahamas?"

  @usingMockedOpenAiService
  Scenario: AI question generation prompt includes depth-two outgoing wiki path
    Given I have a notebook "English practice" with notes:
      | Title       | Details                                                |
      | FarDepthTwo | K2 peak height is 8611 meters.                         |
      | MidDepthTwo | Bridge [[FarDepthTwo]].                                |
      | sedition    | Sedition means incite violence. See [[MidDepthTwo]].   |
    And OpenAI generates this question only when prompt shows depth-two wiki path to Far:
      | Question Stem              | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | How high is K2 in meters? | 8611           | 3776               | 8849               |
    And the note "sedition" was assimilated on day 1
    When I am recalling my note on day 2
    Then I should be asked "How high is K2 in meters?"

  @usingMockedOpenAiService
  Scenario: AI question generation prompt includes folder sibling context
    Given I have a notebook "English practice" with notes:
      | Title       | Details             | Skip Memory Tracking | Folder |
      | FocusFolder | Focus only content  |                      | peers  |
      | SibOne      | sibling one details | true                 | peers  |
      | SibTwo      | sibling two details | true                 | peers  |
    And OpenAI generates this question only when the prompt includes two folder siblings:
      | Question Stem              | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the focus content? | Focus only     | sibling one        | unrelated          |
    And the note "FocusFolder" was assimilated on day 1
    When I am recalling my note on day 2
    Then I should be asked "What is the focus content?"
