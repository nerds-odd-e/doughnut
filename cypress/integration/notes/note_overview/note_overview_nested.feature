Feature: Note overview with nested children
  As a learner, I want to see the notebook including nested children in full view,
  so that I can review my notes in the form of a mindmap.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title            | testingParent | description                                   |
      | Singapore        |               |                                               |
      | History          | Singapore     | This is a description for Singapore's history |
      | Geography        | Singapore     |                                               |
      | Leaving Malaysia | History       | This is a description for leaving Malaysia    |
    And I open "Singapore" note from top level
    And I click on the overview button

  @ignore
  Scenario: View the child notes of a child note in sequential order
    Then I should see the title "Singapore" of the notebook
    Then I should see the child notes "History,Geography" in order
    Then I should see the child notes "History,Leaving Malaysia,Geography" in order

    @ignore
  Scenario: View the description of each note under the title
    Then I should see the note description in between titles "History" and "Leaving Malaysia"