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

  @ignore
  Scenario: the note should be rendered with parent reference
    Given I navigate to to "My Notes/LeSS in Action" note
    When I collapse the children of note "LeSS in Action"
    Then I should see the note "LeSS in Action" with 2 children collapse
    When I expend the children of note "LeSS in Action"
    Then I should see the children notes:
      | topic |
      | TDD   |
      | ATDD  |
