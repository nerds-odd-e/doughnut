Feature: Note edit conflict detection
  As a learner, I want to be notified when other are editing the same field of the same note.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | testingParent  | description         |
      | LeSS in Action |                | An awesome training |
      | team           | LeSS in Action |                     |
      | tech           | LeSS in Action |                     |

  @ignore
  Scenario: Edit a note title
    Given I open note "LeSS in Action"
    And Other person update note "LeSS in Action" to become:
      | title             | description       |
      | LeSS Training     | An awesome training |
    When I edit note title "LeSS in Action" to become "Odd-e CSD"
    Then I should see alert "Conflict detected, change cannot be saved" in the page
    When I dismissed the alert
    Then I should see these notes belonging to the user at the top level of all my notes
      | title     | description         |
      | Odd-e CSD | An awesome training |

  @ignore
  Scenario: Edit a note description
    Given I open note "team"
    And Other person update note "team" to become:
      | title             | description           |
      | Odd-e CSD         | Our best training new |
    When I update note "team" with the description "New description"
    Then I should see alert "Conflict detected, change cannot be saved" in the page
    When I dismissed the alert
    Then I should see these notes belonging to the user at the top level of all my notes
      | title         | description           |
      | team          | New description       |

