Feature: undo note version
  As a learner, I want to be able to undo changes in an existing note. This will restore the previous version of the title and description on the existing note.

  Background:
    Given I've logged in as an existing user

  @ignore
  Scenario: Undo changes in a note title
    Given there are some notes for the current user
      | title    |
      | ABC      |
    When I am editing note "ABC" the field should be pre-filled with
      | Title          |
      | ABC |
    When I update it to become:
      | Title          |
      | DEFG |
    And I click the undo button
    Then I should see these note
      | title     |
      | ABC |
