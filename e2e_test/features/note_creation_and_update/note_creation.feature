Feature: Nested Note creation
  As a learner, I want to maintain my newly acquired knowledge in
  notes, so that I can recall them in the future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with a note "LeSS in Action" and notes:
      | Title | Parent Title   | Folder         |
      | team  | LeSS in Action | LeSS in Action |
      | tech  | LeSS in Action | LeSS in Action |

  Scenario: Create a new note belonging to another note
    When I create a note belonging to "LeSS in Action" with title "Re-quirement"
    Then I should see the note tree in the sidebar
      | note-title   |
      | LeSS in Action |
      | team         |
      | tech         |
      | Re-quirement |
    And I should see folder "LeSS training/LeSS in Action" containing these notes:
      | note-title   |
      | team         |
      | tech         |
      | Re-quirement |

  Scenario: Create a new note with incorrect info
    When I create a note belonging to "LeSS in Action" with title ""
    Then I should see that the note creation is not successful

  Scenario: Create a new note appended last in folder
    When I activate folder "LeSS in Action" in the sidebar and create a new note with title "coordination"
    Then I should see the note tree in the sidebar
      | note-title   |
      | LeSS in Action |
      | team         |
      | tech         |
      | coordination |
    And I should see folder "LeSS training/LeSS in Action" containing these notes:
      | note-title   |
      | team         |
      | tech         |
      | coordination |

  Scenario: Undo creating a new note
    When I create a note belonging to "LeSS in Action" with title "New Note"
    And I undo "create note"
    Then I should see the note "New Note" is marked as deleted
    And I should see folder "LeSS training/LeSS in Action" containing these notes:
      | note-title |
      | team       |
      | tech       |
