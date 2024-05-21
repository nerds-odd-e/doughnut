Feature: Note tree view
  As a learner, I want to browse my notes in a tree view.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent  |
      | LeSS in Action   |                |
      | TDD              | LeSS in Action |
      | ATDD             | LeSS in Action |
      | TPP              | TDD            |
      | Const            | TPP            |
      | Pull             | ATDD           |

  Scenario: a note can have children
    Given I navigate to "My Notes/LeSS in Action" note
    When I collapse the children of note "LeSS in Action"
    Then I should see the note "LeSS in Action" with 2 children collapsed
    When I expand the children of note "LeSS in Action"
    Then I should see the children notes:
      | note-topic |
      | TDD        |
      | ATDD       |

  Scenario: expand side bar to see the note tree
    Given I am on a window 500 * 500
    And I navigate to "My Notes/LeSS in Action" note
    When I expand the side bar
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | ATDD       |

  Scenario: Side bar should show the whole notebook
    When I navigate to "My Notes/LeSS in Action/TDD" note
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | TPP        |
      | ATDD       |
    When I route to the note "ATDD"
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | TPP        |
      | ATDD       |
      | Pull       |

  @ignore
  Scenario: expand and collapse children in the sidebar
    Given I navigate to "My Notes/LeSS in Action" note
    When I expand the children of note "TDD"
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | TPP        |
      | ATDD       |
