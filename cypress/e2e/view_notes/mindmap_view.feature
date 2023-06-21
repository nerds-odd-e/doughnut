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
    And I open the "mindmap" view of note "Singapore"

  Scenario: view the mindmap
    Then I should see the note "Singapore" is 0px * 0px offset the center of the map
    And I should see the notes "History,Geography" are around note "Singapore" and apart from each other
    And The note "History" "should not" have the description indicator
    And The note "Leaving Malaysia" "should" have the description indicator
    When I drag the map by 200px * 100px
    Then I should see the note "Singapore" is 200px * 27px offset the center of the map
    When I zoom in at the "topLeft"
    Then I should see the note "Singapore" is 550px * 223px offset the center of the map
    And I should see the note "Geography" is 235px * 223px offset the center of the map
    And I should see the zoom scale is "150%"
    When I click the zoom indicator
    Then I should see the note "Singapore" is 0px * 0px offset the center of the map
    When I drag the map by 200px * 100px when holding the shift button
    Then I should see the note "Singapore" is 0px * 0px offset the center of the map
    And I should see the note "Geography" is -145px * -44px offset the center of the map

    @mockBrowserTime
  Scenario: highlight a note
    When I click note "History" avoiding the title
    Then I should see the note "History" is "highlighted"
    When I click note "Singapore" avoiding the title
    Then I should see the note "Singapore" is "highlighted"
    And I should see the note "History" is "not highlighted"

  Scenario: view sub notes
    When I open the "mindmap" view of note "History"
    Then I should see the note "History" is 0px * 0px offset the center of the map
    And I should see the note "History" is "highlighted"
    And I should see "Singapore" in breadcrumb with add sibling button
