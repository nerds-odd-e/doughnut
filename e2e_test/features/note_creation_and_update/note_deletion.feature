Feature: Note deletion

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with notes:
      | Title     | Folder               |
      | LeSS in Action | |
      | team      | LeSS in Action       |
      | tech      | LeSS in Action       |
      | TDD       | LeSS in Action/tech  |
      | CI System | LeSS in Action/tech  |

  Scenario: Delete a note
    When I delete note "TDD"
    Then I should see the note "TDD" is marked as deleted

  Scenario: Delete a note will delete its relationships
    Given there is "a part of" relationship between note "TDD" and "tech"
    And I should see "TDD" has relationship "a part of" "tech"
    When I delete note "TDD"
    Then I should see folder "LeSS training/LeSS in Action/tech" containing these notes:
      | note-title |
      | CI System  |
    When I undo "delete note"
    Then I should see "TDD" has relationship "a part of" "tech"

  @ignore
  Scenario: Delete a note then delete its parent and undo
    Given I delete note "TDD" at 13:00
    And I delete note "tech" at 14:00
    When I undo delete note to recover note "tech"
    And I should see folder "LeSS training/LeSS in Action/tech" containing these notes:
      | note-title |
      | CI System  |
    When I undo delete note to recover note "TDD" again
    And I should see folder "LeSS training/LeSS in Action/tech" containing these notes:
      | note-title |
      | CI System  |
      | TDD        |

  @ignore
  Scenario: Delete a note will delete its descendants
    Given I have a notebook "Descendants suite" with notes:
      | Title     | Folder                        |
      | Descendants Test | |
      | parent    | Descendants Test               |
      | child     | Descendants Test/parent        |
      | Unit Test | Descendants Test/parent/child  |
    When I delete note "child"
    Then I should see the note "child" is marked as deleted
    And I should see the note "Unit Test" is marked as deleted
    And I should see folder "Descendants suite/Descendants Test/parent" containing these notes:
      | note-title |

  Scenario: Delete a note will delete its references
    Given I have a notebook "References suite" with notes:
      | Title  | Folder          |
      | References Test | |
      | source | References Test |
      | target | References Test |
    And there is "a part of" relationship between note "source" and "target"
    And I should see "source" has relationship "a part of" "target"
    When I delete note "target"
    And I navigate to References suite/References Test note
    Then I should see folder "References suite/References Test" containing these notes:
      | note-title |
      | source     |
