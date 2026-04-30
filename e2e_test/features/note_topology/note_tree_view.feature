Feature: Note tree view
  As a learner, I want to browse my notes in a tree view.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with a note "LeSS in Action" and notes:
      | Title | Parent Title   | Folder                 |
      | TDD   | LeSS in Action | LeSS in Action         |
      | ATDD  | LeSS in Action | LeSS in Action         |
      | CI    | LeSS in Action | LeSS in Action         |
      | TPP   | TDD            | LeSS in Action/TDD     |
      | Const | TPP            | LeSS in Action/TDD/TPP |
      | Pull  | ATDD           | LeSS in Action/ATDD    |

  Scenario: a note can have children
    Given I navigate to "LeSS training/LeSS in Action" note
    When I collapse the children of note "LeSS in Action"
    Then I should see the note "LeSS in Action" with 3 children collapsed
    When I expand the children of note "LeSS in Action"
    Then I should see the children notes:
      | note-title |
      | TDD        |
      | ATDD       |
      | CI         |

  Scenario: expand side bar to see the note tree
    Given I am on a window 500 * 500
    And I navigate to "LeSS training/LeSS in Action" note
    When I expand the side bar
    Then I should see the note tree in the sidebar
      | note-title     |
      | LeSS in Action |
      | TDD            |
      | ATDD           |
      | CI             |

  Scenario: Side bar should show the whole notebook from TDD
    When I navigate to "LeSS training/LeSS in Action/TDD" note
    Then I should see the note tree in the sidebar
      | note-title     |
      | LeSS in Action |
      | TDD            |
      | TPP            |
      | ATDD           |
      | CI             |

  Scenario: Side bar shows the tree when opening a deep note directly
    When I navigate to "LeSS training/LeSS in Action/TDD/TPP/Const" note
    When I expand the side bar
    Then I should see the note tree in the sidebar
      | note-title     |
      | LeSS in Action |
      | TDD            |
      | TPP            |
      | Const          |
      | ATDD           |
      | CI             |

  Scenario: Side bar should show the whole notebook from ATDD
    Given I navigate to "LeSS training/LeSS in Action/TDD" note
    When I route to the note "ATDD"
    And I expand the children of note "TDD" in the sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | LeSS in Action |
      | TDD            |
      | ATDD           |
      | Pull           |
      | CI             |

  Scenario: Sidebar tree stays populated when opening another note from the sidebar
    Given I navigate to "LeSS training/LeSS in Action/TDD" note
    When I expand the side bar
    Then I should see the note tree in the sidebar
      | note-title     |
      | LeSS in Action |
      | TDD            |
      | TPP            |
      | ATDD           |
      | CI             |
    When I open the note "CI" from the sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | LeSS in Action |
      | TDD            |
      | TPP            |
      | ATDD           |
      | CI             |

  Scenario: expand and collapse children in the sidebar
    Given I navigate to "LeSS training/LeSS in Action" note
    When I expand the children of note "TDD" in the sidebar
    Then I should see the note tree in the sidebar
      | note-title     |
      | LeSS in Action |
      | TDD            |
      | TPP            |
      | ATDD           |
      | CI             |

  @ignore
  Scenario: moving a note within the parent
    When I move the note "CI" up among its siblings
    Then I should see the note "CI" before the note "ATDD" in the sidebar
    And I should see the note "TDD" before the note "CI" in the sidebar
    When I move the note "CI" down among its siblings
    Then I should see the note "ATDD" before the note "CI" in the sidebar
