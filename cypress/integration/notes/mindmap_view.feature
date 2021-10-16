Feature: Mindmap view
  As a learner, I want to see the notebook as a mindmap.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title            | testingParent | description |
      | Singapore        |               |             |
      | History          | Singapore     |             |
      | Geography        | Singapore     |             |
      | Leaving Malaysia | History       | in 1965     |
    And I open "Singapore" note from top level
    And I click on the mindmap view button

  Scenario: View the child notes of a child note in sequential order
    Then I should see the note "Singapore" in the center of the map
    And I should see the notes "History,Geography" are around note "Singapore" and apart from each other
