Feature: Nested Note creation
  As a learner, I want to maintain my newly acquired knowledge in
  notes, so that I can recall them in the future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with notes:
      | Title        | Folder         |
      | Course intro |                |
      | team         | LeSS in Action |
      | tech         | LeSS in Action |

  Scenario: Create a new note under a folder in a notebook
    When I create a note with title "Re-quirement" under the folder "LeSS in Action" in the notebook "LeSS training"
    Then I should see the note tree in the sidebar
      | note-title     |
      | Re-quirement   |
      | team           |
      | tech           |
      | Course intro |
    And I should see folder "LeSS training/LeSS in Action" containing these notes:
      | note-title   |
      | team         |
      | tech         |
      | Re-quirement |

  Scenario: Create a new note with incorrect info
    When I create a note with title "" under the folder "LeSS in Action" in the notebook "LeSS training"
    Then I should see that the note creation is not successful

  Scenario: Create a new note appended last in folder
    When I activate folder "LeSS in Action" in the sidebar and create a new note with title "coordination"
    Then I should see the note tree in the sidebar
      | note-title     |
      | coordination   |
      | team           |
      | tech           |
      | Course intro |
    And I should see folder "LeSS training/LeSS in Action" containing these notes:
      | note-title   |
      | team         |
      | tech         |
      | coordination |

  Scenario: Undo creating a new note
    When I create a note with title "New Note" under the folder "LeSS in Action" in the notebook "LeSS training"
    And I undo "create note"
    Then I should see the note "New Note" is marked as deleted
    And I should see folder "LeSS training/LeSS in Action" containing these notes:
      | note-title |
      | team       |
      | tech       |

  Scenario: Create a folder at notebook root
    When I create a folder named "Top Shelf" while viewing note "Course intro"
    Then I should see sidebar folder "Top Shelf"

  Scenario: Create a nested folder under an existing folder
    When I create a folder named "Deep" while viewing note "team"
    Then I should see sidebar folder "Deep" under open folder "LeSS in Action"
