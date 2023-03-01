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

  Scenario: Create a new note belonging to another note
    When I create a note belonging to "LeSS in Action":
      | Title        | Description                        |
      | Re-quirement | Re-think the way we do requirement |
    And I should see "My Notes/LeSS in Action" with these children
      | note-title   |
      | team         |
      | tech         |
      | Re-quirement |

  Scenario: Create a new note with wrong info
    When I try to create a note belonging to "LeSS in Action":
      | Title |
      |       |
    Then I should see that the note creation is not successful

  @ignore
  Scenario: Create a new note and try to sumbit twice
    When I try to create a note belonging to "tech":
      | Title |
      | integration  |
    And I try to submit again immediately
    Then I should see "integration" in note title
    And I should see "My Notes/LeSS in Action/tech" with these children
      | note-title   |
      | integration  |

  Scenario: Create a new sibling note
    Given I create a note belonging to "LeSS in Action":
      | Title        | Description                        |
      | Re-quirement | Re-think the way we do requirement |
    When I create a sibling note of "Re-quirement":
      | Title     | Description                   | Link Type To Parent |
      | Re-Design | Re-think the way we do design | a specialization of |
    And I should see "My Notes/LeSS in Action" with these children
      | note-title   |
      | Re-quirement |
      | Re-Design    |
      | team         |
      | tech         |
    And On the current page, I should see "LeSS in Action" has link "a specialization of" "Re-Design"

  Scenario: Edit a note
    And I update note "LeSS in Action" to become:
      | Title     | Description       |
      | Odd-e CSD | Our best training |
    Then I should see "Odd-e CSD" in the page
    And I should see these notes belonging to the user at the top level of all my notes
      | title     | description       |
      | Odd-e CSD | Our best training |
