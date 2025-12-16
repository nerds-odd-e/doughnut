Feature: Note deletion

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Title            | Parent Title   |
      | team             | LeSS in Action |
      | tech             | LeSS in Action |
      | TDD              | tech           |
      | CI System        | tech           |

  Scenario: Delete a note
    When I delete note "TDD"
    Then I should see the note "TDD" is marked as deleted

  Scenario: Delete a note will delete its links
    Given there is "a part of" relationship between note "TDD" and "tech"
    And I should see "TDD" has relationship "a part of" "tech"
    When I delete note "TDD"
    Then I should see "LeSS in Action/tech" with these children
      | note-title |
      | CI System  |
    When I undo "delete note"
    Then I should see "TDD" has relationship "a part of" "tech"

  Scenario: Delete a note then delete its parent and undo
    Given I delete note "TDD" at 13:00
    And I delete note "tech" at 14:00
    When I undo delete note to recover note "tech"
    And I should see "LeSS in Action/tech" with these children
      | note-title |
      | CI System  |
    When I undo delete note to recover note "TDD" again
    And I should see "LeSS in Action/tech" with these children
      | note-title |
      | CI System  |
      | TDD        |

  Scenario: Delete a note will delete its descendants
    Given I have a notebook with head note "Descendants Test" and notes:
      | Title     | Parent Title       |
      | parent    | Descendants Test   |
      | child     | parent             |
      | Unit Test | child              |
    When I delete note "child"
    Then I should see the note "child" is marked as deleted
    And I should see the note "Unit Test" is marked as deleted
    And I should see "Descendants Test/parent" with these children
      | note-title |

  Scenario: Delete a note will delete its references
    Given I have a notebook with head note "References Test" and notes:
      | Title       | Parent Title     |
      | source      | References Test  |
      | target      | References Test  |
    And there is "a part of" relationship between note "source" and "target"
    And I should see "source" has relationship "a part of" "target"
    When I delete note "target"
    And I navigate to References Test note
    Then I should see "References Test" with these children
      | note-title |
      | source     |
