@usingMockedOpenAiService
Feature: Refine note after answered MCQ
  As a learner, after answering an MCQ on a contentful note,
  I want to open Refine note and see question-led refinement layout items already selected,
  so I can extract or remove the content that led to the question.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                                                       |
      | sedition | Sedition means incite violence. Extra point A. Extra point B. |
    And OpenAI generates refinement layout:
      | id | text                           | parent | alreadyExtracted | ledToQuestion |
      | p1 | Sedition means incite violence |        |                  | true          |
      | p2 | Extra point A                  |        |                  |               |
      | p3 | Extra point B                  |        |                  |               |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And OpenAI evaluates the question as legitimate
    And the note "sedition" was assimilated on day 1

  Scenario: Question-led refinement layout items are preselected when refining after MCQ
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    And I open Refine note from the answered question
    Then refinement layout items "Sedition means incite violence" should be selected
    And refinement layout items "Extra point A" and "Extra point B" should not be selected
