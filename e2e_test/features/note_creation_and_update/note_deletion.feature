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

  Scenario: Deleting a note in a folder navigates to that folder page
    When I delete note "TDD"
    Then I should be on a notebook folder page in the browser

  Scenario: Deleting a note at notebook root navigates to the notebook page
    When I delete note "LeSS in Action"
    Then I should be on the notebook root page in the browser

  Scenario: Deleting a note leaves folder peers; undo restores relationships
    Given there is "a part of" relationship between note "TDD" and "tech" in notebook "LeSS training"
    And I should see "TDD" has relationship "a part of" "tech"
    When I delete note "TDD" and leave references as dead links
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

  Scenario: Deleting a note does not remove other notes by structural descent
    Given I have a notebook "Descendants suite" with notes:
      | Title     | Folder                        |
      | Descendants Test | |
      | parent    | Descendants Test               |
      | child     | Descendants Test/parent        |
      | Unit Test | Descendants Test/parent/child  |
    When I delete note "child"
    Then I should see the note "child" is marked as deleted
    And I should see folder "Descendants suite/Descendants Test/parent/child" containing these notes:
      | note-title |
      | Unit Test  |

  Scenario: Deleting a note leaves inbound relationship notes and folder peers
    Given I have a notebook "References suite" with notes:
      | Title  | Folder          |
      | References Test | |
      | source | References Test |
      | target | References Test |
    And there is "a part of" relationship between note "source" and "target" in notebook "References suite"
    And I should see "source" has relationship "a part of" "target"
    When I delete note "target" and leave references as dead links
    And I navigate to References suite/References Test note
    Then I should see folder "References suite/References Test" containing these notes:
      | note-title |
      | source     |
    And I should see note "References suite/References Test/source" has relationship "a part of" "target"

  Scenario: Deleting a referenced note can remove it from reference properties while leaving body links dead
    Given I have a notebook "Reference cleanup suite" with notes:
      | Title  | Folder            |
      | Reference Cleanup | |
      | source | Reference Cleanup |
      | target | Reference Cleanup |
    And I update note "source" content using markdown to become:
      """
      ---
      target: "[[target]]"
      ---
      Body keeps [[target]]
      """
    When I delete note "target" and remove it from properties of references
    And I navigate to Reference cleanup suite/Reference Cleanup/source note
    Then I should not see rich note property "target"
    And I should see wiki link "target" as a dead link
