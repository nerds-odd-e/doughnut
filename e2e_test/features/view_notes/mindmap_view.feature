Feature: Mindmap view
  As a learner, I want to see the notebook as a mindmap.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | title            | testingParent | description |
      | Singapore        |               |             |
      | History          | Singapore     |             |
      | Geography        | Singapore     |             |
      | Leaving Malaysia | History       | in 1965     |
      | Terry came       | History       | in 2014     |
    And I open the "mindmap" view of note "Singapore"

  Scenario: view the mindmap
    And I should see the notes "History,Geography" are around note "Singapore" and apart from each other
    And The note "History" "should not" have the description indicator
    And The note "Leaving Malaysia" "should" have the description indicator
    When I drag the map by 200px * 100px
    When I zoom in at the "topLeft"
    And I should see the zoom scale is "150%"
    When I click the zoom indicator
    When I drag the map by 200px * 100px when holding the shift button

    @mockBrowserTime
  Scenario: highlight a note
    When I click note "History" avoiding the title
    When I click note "Singapore" avoiding the title

  Scenario: view sub notes
    When I open the "mindmap" view of note "History"
