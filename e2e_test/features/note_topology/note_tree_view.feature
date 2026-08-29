Feature: Note tree view
  As a learner, I want to browse my notes in a tree view.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with notes:
      | Title          | Folder                 |
      | LeSS in Action |                        |
      | TDD            | LeSS in Action         |
      | ATDD           | LeSS in Action         |
      | CI             | LeSS in Action         |
      | TPP            | LeSS in Action/TDD     |
      | Const          | LeSS in Action/TDD/TPP |
      | Pull           | LeSS in Action/ATDD    |

  Scenario: Open sidebar on a narrow window
    Given I am on a window 500 * 500
    And I route to the note "LeSS in Action"
    When I open the notebook sidebar
    Then I should see sidebar folder "LeSS in Action"

  Scenario: Sidebar shows the whole notebook from TDD
    When I route to the note "TDD"
    And I expand the children of note "TDD" in the sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | TPP            |
      | ATDD           |
      | CI             |
      | TDD            |
      | LeSS in Action |

  Scenario: Sidebar shows the tree when opening a deep note directly
    When I route to the note "Const"
    And I open the notebook sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | Const          |
      | TPP            |
      | ATDD           |
      | CI             |
      | TDD            |
      | LeSS in Action |

  Scenario: Sidebar shows the whole notebook from ATDD
    Given I route to the note "TDD"
    When I route to the note "ATDD"
    And I expand the children of note "TDD" in the sidebar
    And I expand the children of note "ATDD" in the sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | Pull           |
      | TPP            |
      | ATDD           |
      | CI             |
      | TDD            |
      | LeSS in Action |

  Scenario: Sidebar tree stays populated when opening another note from the sidebar
    Given I route to the note "TDD"
    When I open the notebook sidebar
    And I expand the children of note "TDD" in the sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | TPP            |
      | ATDD           |
      | CI             |
      | TDD            |
      | LeSS in Action |
    When I open the note "CI" from the sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | TPP            |
      | ATDD           |
      | CI             |
      | TDD            |
      | LeSS in Action |

  Scenario: Expand nested children in the sidebar
    Given I route to the note "LeSS in Action"
    When I expand the children of note "LeSS in Action" in the sidebar
    And I expand the children of note "TDD" in the sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | TPP            |
      | ATDD           |
      | CI             |
      | TDD            |
      | LeSS in Action |
