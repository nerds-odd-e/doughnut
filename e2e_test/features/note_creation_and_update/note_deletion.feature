Feature: Note trash

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with notes:
      | Title          | Folder              |
      | LeSS in Action |                     |
      | team           | LeSS in Action      |
      | tech           | LeSS in Action      |
      | TDD            | LeSS in Action/tech |
      | CI System      | LeSS in Action/tech |

  Scenario: Trash a note
    When I trash note "TDD"
    Then I should see the note "TDD" is in trash

  Scenario: Trashing a note in a folder opens that folder page
    When I trash note "TDD"
    Then I should be on a notebook folder page

  Scenario: Trashing a note at notebook root opens the notebook page
    When I trash note "LeSS in Action"
    Then I should be on the notebook root page

  Scenario: Trashing a note leaves folder peers
    Given there is "a part of" relationship between note "TDD" and "tech" in notebook "LeSS training"
    And I should see "TDD" has relationship "a part of" "tech"
    When I trash note "TDD" and leave references as dead wiki links
    Then I should see folder "LeSS training/LeSS in Action/tech" containing these notes:
      | note-title |
      | CI System  |

  Scenario: Immediate Undo restores a trashed note and its relationships
    Given there is "a part of" relationship between note "TDD" and "tech" in notebook "LeSS training"
    And I trash note "TDD" and leave references as dead wiki links
    When I undo "trash note"
    Then I should see "TDD" has relationship "a part of" "tech"

  @ignore
  Scenario: Trash a note then trash its parent and undo
    Given I trash note "TDD" at 13:00
    And I trash note "tech" at 14:00
    When I undo trash note to recover note "tech"
    And I should see folder "LeSS training/LeSS in Action/tech" containing these notes:
      | note-title |
      | CI System  |
    When I undo trash note to recover note "TDD"
    And I should see folder "LeSS training/LeSS in Action/tech" containing these notes:
      | note-title |
      | CI System  |
      | TDD        |

  Scenario: Trashing a note does not remove other notes by structural descent
    Given I have a notebook "Descendants suite" with notes:
      | Title            | Folder                        |
      | Descendants Test |                               |
      | parent           | Descendants Test              |
      | child            | Descendants Test/parent       |
      | Unit Test        | Descendants Test/parent/child |
    When I trash note "child"
    Then I should see the note "child" is in trash
    And I should see folder "Descendants suite/Descendants Test/parent/child" containing these notes:
      | note-title |
      | Unit Test  |

  Scenario: Trashing a note leaves inbound relationship notes and folder peers
    Given I have a notebook "References suite" with notes:
      | Title           | Folder          |
      | References Test |                 |
      | source          | References Test |
      | target          | References Test |
    And there is "a part of" relationship between note "source" and "target" in notebook "References suite"
    And I should see "source" has relationship "a part of" "target"
    When I trash note "target" and leave references as dead wiki links
    And I navigate to References suite/References Test note
    Then I should see folder "References suite/References Test" containing these notes:
      | note-title |
      | source     |
    And I should see note "References suite/References Test/source" has relationship "a part of" "target"

  Scenario: Trashing a referenced note can remove it from reference properties while leaving body wiki links dead
    Given I have a notebook "Reference cleanup suite" with notes:
      | Title             | Folder            |
      | Reference Cleanup |                   |
      | target            | Reference Cleanup |
    And I have a note "source" under notebook "Reference cleanup suite" in folder "Reference Cleanup" with content:
      """
      ---
      target: "[[target]]"
      ---
      Body keeps [[target]]
      """
    When I trash note "target" and remove it from properties of references
    And I navigate to Reference cleanup suite/Reference Cleanup/source note
    Then I should not see rich note property "target"
    And I should see wiki link "target" as a dead wiki link

  @mockBrowserTime
  Scenario: Recover an older trashed note into an existing active folder after a remount
    Given I have a notebook "Biology study" with notes:
      | Title | Folder  | Content      |
      | Cells | Biology | Mitochondria |
    When I trash note "Cells"
    And I jump to the notebook "Biology study"
    And I reload the notebook page
    And I expand the children of note "_trash" in the sidebar
    And I open the folder page for "Biology" under open parent "_trash"
    Then I should see the folder page is in trash
    When I open the note "Cells" from the sidebar
    Then I should see the current note is in trash
    And the note content should include "Mitochondria"
    When I move the current note under folder "Biology" in notebook "Biology study"
    Then I should see the current note is not in trash
    And the note content should include "Mitochondria"
    And I should see folder "Biology study/Biology" containing these notes:
      | note-title |
      | Cells      |

  @mockBrowserTime
  Scenario: Recover a trashed note to notebook root when the original parent is absent
    Given I have a notebook "Biology study" with notes:
      | Title | Folder          | Content      |
      | Cells | _trash/Biology | Mitochondria |
    When I jump to the notebook "Biology study"
    And I reload the notebook page
    And I expand the children of note "_trash" in the sidebar
    And I open the folder page for "Biology" under open parent "_trash"
    Then I should see the folder page is in trash
    When I open the note "Cells" from the sidebar
    Then I should see the current note is in trash
    When I move the current note to notebook "Biology study" root
    Then I should see the current note is not in trash
    When I open the notebook "Biology study" from the notebook catalog
    Then I should see the note tree in the sidebar
      | note-title |
      | Cells      |
    And I should not see sidebar folder "Biology"

  @mockBrowserTime
  Scenario: Recover a trashed note when a new note has taken its old active name
    Given I have a notebook "Recovery suite" with notes:
      | Title  | Folder |
      | Origin |        |
      | Old    | Origin |
    When I trash note "Old"
    And I create a note with title "Old" under the folder "Origin" in the notebook "Recovery suite"
    And I jump to the notebook "Recovery suite"
    And I reload the notebook page
    And I expand the children of note "_trash" in the sidebar
    And I open the folder page for "Origin" under open parent "_trash"
    Then I should see the folder page is in trash
    When I open the note "Old" from the sidebar
    Then I should see the current note is in trash
    When I move the current note to notebook "Recovery suite" root
    Then I should see the current note is not in trash
    And I should see folder "Recovery suite/Origin" containing these notes:
      | note-title |
      | Old        |
