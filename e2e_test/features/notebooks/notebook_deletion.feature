Feature: Notebook deletion

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Topic | Parent Topic   |
      | team  | LeSS in Action |
      | tech  | LeSS in Action |

  Scenario: Delete a notebook and undo
    Given I initial review "LeSS in Action"
    When I delete notebook "LeSS in Action"
    Then I should not see note "LeSS in Action" at the top level of all my notes
    When I undo "delete note"
    Then the deleted notebook with topic "LeSS in Action" should be restored
    And there should be no more undo to do
