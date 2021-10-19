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
      | Terry came       | History       | in 2014     |
    And I open "Singapore" note from top level
    And I click on the mindmap view button

  Scenario: View the child notes of a child note in sequential order
    Then I should see the note "Singapore" is 0px * 0px offset the center of the map
    And I should see the notes "History,Geography" are around note "Singapore" and apart from each other
    When I drag the map by 200px * 100px
    Then I should see the note "Singapore" is 200px * 20px offset the center of the map
    When I zoom in at the top left corner
    Then I should see the note "Singapore" is 550px * 214px offset the center of the map
    Then I should see the note "Geography" is 235px * 214px offset the center of the map


