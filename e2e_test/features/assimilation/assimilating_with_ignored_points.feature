Feature: Assimilation With Ignored Points
  As a learner, I want to select the points to ignore in the checklist when assimilating my note.
  As a learner, I want to selected points to be ignored in the checklist when generating questions for my note.
  So that I can focus on the important points and not be distracted by the irrelevant points.

  Background: 
    Given I am logged in as an existing user
    And I have a notebook with the head note "Countries" which skips review
    And there are some notes:
      | Title    | Details                        | Skip Memory Tracking | Parent Title |
      | Netherlands | The Netherlands is a country in Europe and also called Holland| false                | Countries      |
 
  @ignore
  Scenario: AI generated question - ignore checklist topic in question options
    When I am assimilating new note on day 1
    And one of the checklist topic is selected to ignore "also called Holland" and assimilate the note
    Then the question generated for the note "Netherlands" should not include "Holland"

