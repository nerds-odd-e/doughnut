Feature: Nested Note creation
  As a learner, I want to maintain my newly acquired knowledge in
  notes, so that I can review them in the future.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | topic          | testingParent  | description         |
      | LeSS in Action |                | An awesome training |
      | team           | LeSS in Action |                     |
      | tech           | LeSS in Action |                     |

  Scenario: Create a new note belonging to another note
    When I create a note belonging to "LeSS in Action":
      | Topic        | Description                        |
      | Re-quirement | Re-think the way we do requirement |
    And I should see "My Notes/LeSS in Action" with these children
      | note-topic   |
      | team         |
      | tech         |
      | Re-quirement |

  Scenario: Create a new note with incorrect info
    When I try to create a note belonging to "LeSS in Action":
      | Topic |
      |       |
    Then I should see that the note creation is not successful

  Scenario: Create a new sibling note
    Given I create a note belonging to "LeSS in Action":
      | Topic        | Description                        |
      | Re-quirement | Re-think the way we do requirement |
    When I create a sibling note of "Re-quirement":
      | Topic     | Description                   | Link Type To Parent |
      | Re-Design | Re-think the way we do design | a specialization of |
    And I should see "My Notes/LeSS in Action" with these children
      | note-topic   |
      | Re-quirement |
      | Re-Design    |
      | team         |
      | tech         |
    And On the current page, I should see "LeSS in Action" has link "a specialization of" "Re-Design"
