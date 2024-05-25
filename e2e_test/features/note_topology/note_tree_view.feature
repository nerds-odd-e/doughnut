Feature: Note tree view
  As a learner, I want to browse my notes in a tree view.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent  |
      | LeSS in Action   |                |
      | TDD              | LeSS in Action |
      | ATDD             | LeSS in Action |
      | CI               | LeSS in Action |
      | TPP              | TDD            |
      | Const            | TPP            |
      | Pull             | ATDD           |

  Scenario: a note can have children
    Given I navigate to "My Notes/LeSS in Action" note
    When I collapse the children of note "LeSS in Action"
    Then I should see the note "LeSS in Action" with 3 children collapsed
    When I expand the children of note "LeSS in Action"
    Then I should see the children notes:
      | note-topic |
      | TDD        |
      | ATDD       |
      | CI         |

  Scenario: expand side bar to see the note tree
    Given I am on a window 500 * 500
    And I navigate to "My Notes/LeSS in Action" note
    When I expand the side bar
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | ATDD       |
      | CI         |

  Scenario: Side bar should show the whole notebook
    When I navigate to "My Notes/LeSS in Action/TDD" note
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | TPP        |
      | ATDD       |
      | CI         |
    When I route to the note "ATDD"
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | TPP        |
      | ATDD       |
      | Pull       |
      | CI         |

  Scenario: expand and collapse children in the sidebar
    Given I navigate to "My Notes/LeSS in Action" note
    When I expand the children of note "TDD" in the sidebar
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | TPP        |
      | ATDD       |
      | CI         |

  Scenario: moving a note within the parent
    When I move the note "CI" up among its siblings
    Then I should see the note "CI" before the note "ATDD" in the sidebar
    And I should see the note "TDD" before the note "CI" in the sidebar
    When I move the note "CI" up among its siblings
    Then I should see the note "CI" before the note "TDD" in the sidebar
