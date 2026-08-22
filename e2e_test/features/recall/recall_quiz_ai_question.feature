@usingMockedOpenAiService
Feature: AI recall quiz
  As a learner, I want AI-generated quizzes in recall to help and gamify recall.

  Background:
    Given I am logged in as an existing user
    And OpenAI evaluates the question as legitimate

  Scenario: AI generated question - incorrect answer
    Given I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | sedition | Sedition means incite violence |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   | to stay silent     |
    And the note "sedition" was assimilated on day 1
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect

  Scenario: AI question generation includes wiki-linked, depth-two wiki path, and folder-sibling focus context
    Given I have a notebook "Focus context practice" with notes:
      | Title       | Content                                                | Folder |
      | Bahamas     | The Bahamas is an archipelago in the Atlantic.         |        |
      | FarDepthTwo | K2 peak height is 8611 meters.                         |        |
      | MidDepthTwo | Bridge [[FarDepthTwo]].                                |        |
      | WikiRecall  | Sedition means incite violence. Also see [[Bahamas]].  |        |
      | DepthRecall | Sedition means incite violence. See [[MidDepthTwo]].   |        |
      | SibOne      | sibling one body                                       | peers  |
      | SibTwo      | sibling two body                                       | peers  |
      | FocusFolder | Focus only content                                     | peers  |
    And the notes "SibOne, SibTwo" are skipped from the assimilation sequence
    And OpenAI generates these MCQs when focus context matches depth-two wiki path, folder siblings, and wiki-linked Bahamas note:
      | Question Stem              | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | How high is K2 in meters?  | 8611           | 3776               | 8849               | 5895               |
      | What is the focus content? | Focus only     | sibling one        | unrelated          | sibling two        |
      | What is the Bahamas?       | An archipelago | A continent        | An act of sedition | A mountain range   |
    And the notes "WikiRecall, DepthRecall, FocusFolder" are assimilated on day 1
    When I visit recall waiting for 3 due recall prompts on day 2
    Then OpenAI Responses POST bodies include wiki-linked, depth-two wiki path, and folder-sibling focus context prompts
