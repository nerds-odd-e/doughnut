Feature: See recent note updates
  As a learner, I want to see which of my notes are recently updated,
  so that I can focus on newly updated notes.

  Background:
    Given I am logged in as an existing user
    And it is 100 hours ago on the server
    And I have a notebook "World atlas" with notes:
      | Title  | Content           |
      | Japan  |                   |
      | Berlin | Berlin has a wall |
    And it is 24 hours ago on the server

  Scenario: Updating note content makes it appear newer
    When I update note "Berlin" with content "Berlin had a wall"
    Then I should see that "Berlin" is newer than "Japan"

  Scenario: Saving unchanged content does not make a note appear newer
    When I update note "Berlin" with content "Berlin has a wall"
    Then I should see that "Berlin" is as recent as "Japan"
