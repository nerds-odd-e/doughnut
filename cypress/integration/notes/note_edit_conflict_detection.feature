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
    And I open note "LeSS in Action"
    When Other update note "LeSS in Action" to become:
      | title             | description       |
      | LeSS Training     | An awesome training |
    And I edit note title "LeSS in Action" to become "Odd-e CSD"
    Then I should see alert "Conflict detected, change cannot be saved" in the page
    Then I dismissed the alert
    And I should see these notes belonging to the user at the top level of all my notes
      | title     | description       |
      | LeSS in Action | Our best training |

  @ignore
  Scenario: Edit a note description
    And I open note "Odd-e CSD"
    When Other update note "Odd-e CSD" to become
      | old_title      | title             | description           |
      | Odd-e CSD      | Odd-e CSD         | Our best training new |
    And I update note "Odd-e CSD" with the description "New description"
    Then I should see alert "Conflict detected, change cannot be saved" in the page
    And I should see these notes belonging to the user at the top level of all my notes
      | title         | description           |
      | Odd-e CSD     | Our best training new |

