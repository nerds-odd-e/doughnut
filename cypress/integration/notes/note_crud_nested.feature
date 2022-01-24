Feature: Nested Note CRUD
  As a learner, I want to maintain my newly acquired knowledge in
  notes, so that I can review them in the future.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | testingParent  | description         |
      | LeSS in Action |                | An awesome training |
      | team           | LeSS in Action |                     |
      | tech           | LeSS in Action |                     |

  Scenario: Create a new note belonging to another node
    When I create note belonging to "LeSS in Action":
      | Title        | Description                        |
      | Re-quirement | Re-think the way we do requirement |
    And I open "LeSS in Action" note from top level
    Then I should see "LeSS in Action" in note title
    And I should see these notes as children
      | note-title   |
      | Re-quirement |
    When I am creating note under "LeSS in Action/Re-quirement"

  Scenario: Create a new note with wrong info
    When I create note belonging to "LeSS in Action":
      | Title |
      |       |
    Then I should see that the note creation is not successful

  Scenario: Create a new sibling note
    Given I create note belonging to "LeSS in Action":
      | Title        | Description                        |
      | Re-quirement | Re-think the way we do requirement |
    When I create a sibling note of "Re-quirement":
      | Title     | Description                   | Link Type To Parent |
      | Re-Design | Re-think the way we do design | a specialization of |
    When I open "LeSS in Action" note from top level
    And I should see these notes as children
      | note-title   |
      | Re-quirement |
      | Re-Design    |
    And On the current page, I should see "LeSS in Action" has link "a specialization of" "Re-Design"

  Scenario: Edit a note
    And I update note "LeSS in Action" to become:
      | Title     | Description       |
      | Odd-e CSD | Our best training |
    Then I should see "Odd-e CSD" in the page
    And I should see these notes belonging to the user at the top level of all my notes
      | title     | description       |
      | Odd-e CSD | Our best training |

  Scenario: Delete a note
    Given I initial review "LeSS in Action"
    When I delete top level note "LeSS in Action"
    Then I should not see note "LeSS in Action" at the top level of all my notes
