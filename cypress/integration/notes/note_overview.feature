Feature: Note overview
  As a learner, I want to see the notebook in full view,
  so that I can review my notes in the form of a mindmap.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title        | testingParent | description                            |
      | Sedation     |               |                                        |
      | Child note 1 | Sedation      | This is a description for Child note 1 |
      | Child note 2 | Sedation      |                                        |
    And I open "Sedation" note from top level
    And I click on the overview button

  @ignore
  Scenario: View the title of the notebook
    Then I should see the title "Sedation" of the notebook

  @ignore
  Scenario: View the child notes in sequential order
    Then I should see the child note "Child note 1", "Child note 2" in order

  @ignore
  Scenario: View the child notes of a child note in sequential order
    When there is a child note for a note
      | title                      | testingParent | description                                          |
      | Child note of Child note 1 | Child note 1  | This is a description for Child note of Child note 1 |
    Then I should see the child note "Child note 1", "Child note of Child note 1", "Child note 2" in order

  @ignore
  Scenario: View the description of each note under the title
    Then I should see the note description in between titles "Child note 1" and "Child note of Child note 1"