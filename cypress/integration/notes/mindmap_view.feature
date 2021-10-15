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
    Then I should see the title "Singapore" of the notebook
    And I should see the child notes "History,Geography" in order
    And I should see the child notes "History,Leaving Malaysia,Geography" in order
    And I should see "Top" in breadcrumb
    When I double click "Singapore" and edit the description to "Founded in 1819"
    Then I should see "Founded in 1819" in the page
