Feature: Note overview
  As a learner, I want to see the notebook in full view,
  so that I can review my notes in the form of a mindmap.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title     | testingParent | description                                   |
      | Singapore |               |                                               |
      | History   | Singapore     | This is a description for Singapore's history |
      | Geography | Singapore     |                                               |
    And I open "Singapore" note from top level
    And I click on the overview button

  Scenario: View the title of the notebook
    Then I should see the title "Singapore" of the notebook

  Scenario: View the child notes in sequential order
    Then I should see the child notes "History,Geography" in order

  @ignore
  Scenario: View the child notes of a child note in sequential order
    When there is a child note for a note
      | title            | testingParent | description                                |
      | Leaving Malaysia | History       | This is a description for leaving Malaysia |
    Then I should see the child notes "History,Leaving Malaysia,Geography" in order

  @ignore
  Scenario: View the description of each note under the title
    Then I should see the note description in between titles "History" and "Leaving Malaysia"