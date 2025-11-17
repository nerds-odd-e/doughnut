Feature: Nested Note creation
  As a learner, I want to maintain my newly acquired knowledge in
  notes, so that I can review them in the future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Title | Parent Title   |
      | team  | LeSS in Action |
      | tech  | LeSS in Action |

  Scenario: Create a new note belonging to another note
    When I create a note belonging to "LeSS in Action" with title "Re-quirement"
    Then I should see the note tree in the sidebar
      | note-title   |
      | team         |
      | tech         |
      | Re-quirement |
    And I should see "LeSS in Action" with these children
      | note-title   |
      | team         |
      | tech         |
      | Re-quirement |

  Scenario: Create a new note with incorrect info
    When I create a note belonging to "LeSS in Action" with title ""
    Then I should see that the note creation is not successful

  Scenario: Create a new note as next sibling
    When I create a note after "team" with title "coordination"
    Then I should see the note tree in the sidebar
      | note-title   |
      | team         |
      | coordination |
      | tech         |
    And I should see "LeSS in Action" with these children
      | note-title   |
      | team         |
      | coordination |
      | tech         |
