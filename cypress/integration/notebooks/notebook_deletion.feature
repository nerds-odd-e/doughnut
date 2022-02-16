Feature: Notebook deletion

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | testingParent  | description         |
      | LeSS in Action |                | An awesome training |
      | team           | LeSS in Action |                     |
      | tech           | LeSS in Action |                     |

  Scenario: Delete a notebook and undo
    Given I initial review "LeSS in Action"
    When I delete notebook "LeSS in Action"
    Then I should not see note "LeSS in Action" at the top level of all my notes
    When I click undo delete on snackbar
    Then the deleted notebook with title "LeSS in Action" should be restored
