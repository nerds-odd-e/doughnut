Feature: Note overview
  As a learner, I want to see the notebook in full view,
  so that I can review my notes in the form of a mindmap.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
    And I open "Sedation" note from top level
    And I click on the overview button

  @ignore
  Scenario: View the title of the notebook in the overview page
    Then I should see the title "Sedation"

  @ignore
  Scenario: View the details of the note
    Then I should see the details below the title "Sedation"

  @ignore
  Scenario: View the child notes in sequential order
    When I create a child note "Child note 1"
    And I create a child note "Child note 2"
    Then I should see the child note "Child note 1", "Child note 2" in order

  @ignore
  Scenario: View the child notes of a child note in sequential order
    When I open "Child note 1" note in "Sedation" notebook
    And I create a child note "Child note of Child note 1"
    And I open "Sedation" note from top level
    Then I should see the child note "Child note 1", "Child note of Child note 1", "Child note 2" in order