Feature: Notebook deletion

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with a note "LeSS in Action" and notes:
      | Title | Parent Title   |
      | team  | LeSS in Action |
      | tech  | LeSS in Action |

  Scenario: Delete a notebook and undo
    Given I assimilate the note "LeSS in Action" via more options
    When I delete notebook "LeSS in Action"
    Then I should not see "LeSS in Action" in my notebooks
    When I undo delete note to recover note "LeSS in Action"
    Then the deleted notebook with title "LeSS in Action" should be restored
    And there should be no more undo to do
