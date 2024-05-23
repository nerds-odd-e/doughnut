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

  Scenario: expand and collapse children in the sidebar
    Given I navigate to "My Notes/LeSS in Action" note
    When I expand the children of note "TDD" in the sidebar
    Then I should see the note tree in the sidebar
      | note-topic |
      | TDD        |
      | TPP        |
      | ATDD       |

  @focus
  Scenario: auto scroll to the selected note
    Given there are some notes for the current user:
      | topicConstructor | testingParent  |
      | Numbers          |                |
      | 1                | Numbers        |
      | 2                | Numbers        |
      | 3                | Numbers        |
      | 4                | Numbers        |
      | 5                | Numbers        |
      | 6                | Numbers        |
      | 7                | Numbers        |
      | 8                | Numbers        |
      | 9                | Numbers        |
      | 10               | Numbers        |
      | 11               | Numbers        |
      | 12               | Numbers        |
      | 13               | Numbers        |
      | 14               | Numbers        |
      | 15               | Numbers        |
      | 16               | Numbers        |
      | 17               | Numbers        |
      | 18               | Numbers        |
      | 19               | Numbers        |
      | 20               | Numbers        |
      | 21               | Numbers        |
      | 22               | Numbers        |
      | 23               | Numbers        |
      | 24               | Numbers        |
      | 25               | Numbers        |
      | 26               | Numbers        |
      | 27               | Numbers        |
      | 28               | Numbers        |
      | 29               | Numbers        |
      | 30               | Numbers        |
      | 31               | Numbers        |
      | 32               | Numbers        |
      | 33               | Numbers        |
      | 34               | Numbers        |
      | 35               | Numbers        |
      | 36               | Numbers        |
      | 37               | Numbers        |
      | 38               | Numbers        |
      | 39               | Numbers        |
      | 40               | Numbers        |
      | 41               | Numbers        |
      | 42               | Numbers        |
      | 43               | Numbers        |
    When I navigate to "My Notes/Numbers/42" note
    Then I should see the note "42" highlighted in the sidebar
