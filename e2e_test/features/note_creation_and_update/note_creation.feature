Feature: Nested Note creation
  As a learner, I want to maintain my newly acquired knowledge in
  notes, so that I can review them in the future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Topic | Parent Topic   |
      | team  | LeSS in Action |
      | tech  | LeSS in Action |

  Scenario: Create a new note belonging to another note
    When I create a note belonging to "LeSS in Action":
      | Topic        |
      | Re-quirement |
    Then I should see the note tree in the sidebar
      | note-topic   |
      | team         |
      | tech         |
      | Re-quirement |
    And I should see "My Notes/LeSS in Action" with these children
      | note-topic   |
      | team         |
      | tech         |
      | Re-quirement |

  Scenario: Create a new note with incorrect info
    When I create a note belonging to "LeSS in Action":
      | Topic |
      |       |
    Then I should see that the note creation is not successful

  Scenario: Create a new note as next sibling
    When I create a note after "team":
      | Topic        |
      | coordination |
    Then I should see the note tree in the sidebar
      | note-topic   |
      | team         |
      | coordination |
      | tech         |
    And I should see "My Notes/LeSS in Action" with these children
      | note-topic   |
      | team         |
      | coordination |
      | tech         |
