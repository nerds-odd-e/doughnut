Feature: Undo Note Edit
  As a learner, I want to undo editing for single user,
  with title and description only within a session.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | description         |
      | LeSS in Action | An awesome training |

  Scenario: Edit a note title and edit description
    And I update note title "LeSS in Action" to become "Odd-e CSD"
    And I should see "Odd-e CSD" in the page
    And I update note "Odd-e CSD" description from "An awesome training" to become "A super awesome training"
    And I should see "A super awesome training" in the page
    When I undo "editing"
    Then I should see "An awesome training" in the page
    When I undo "editing" again
    Then I should see "LeSS in Action" in the page
    And there should be no more undo to do
