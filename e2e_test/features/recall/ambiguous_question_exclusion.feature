@usingMockedOpenAiService
Feature: Ambiguous question exclusion
  As a learner, I want to exclude an attempt on a question whose wording did not
  support a unique answer, so that a wrong mark I did not earn stops shaping my
  recall schedule.

  Background:
    Given I am logged in as an existing user
    And OpenAI evaluates the question as legitimate

  Scenario: The learner confirms the tutor's proposal to exclude an ambiguous attempt
    Given I have a notebook "English practice" with notes:
      | Title   | Content                                                    |
      | used to | "I used to live in Tokyo" means I lived there in the past. |
    And OpenAI generates this question:
      | Question Stem                                               | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | Choose the grammatically correct completion: I ___ in Tokyo | used to live   | am used to living  | use to living      | am use to live     |
    And OpenAI will reply below for user messages:
      | user message            | response type   | tool                               | assistant reply                                      |
      | Why is my answer wrong? | requires action | AmbiguousQuestionExclusionProposal | {"reason": "Both completions are grammatical here."} |
    And the note "used to" was assimilated on day 1
    When I visit recall for a due recall prompt on day 2
    And I choose answer "am used to living"
    And I ask private recall help "Why is my answer wrong?"
    And I confirm the tutor's proposal to exclude my attempt because "Both completions are grammatical here."
    Then my answered question should read as excluded for an ambiguous question because "Both completions are grammatical here."
    When I leave private recall help and view the memory tracker
    Then the recall history should show the "AGAIN" attempt as excluded because "Both completions are grammatical here."
    And the next recall time should be the assimilation time
    And private recall help for that attempt should still show it as excluded because "Both completions are grammatical here."
