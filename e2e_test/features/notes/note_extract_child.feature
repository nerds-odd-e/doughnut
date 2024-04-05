Feature: Extract Child Note

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "parent_note"
    And I update note "parent_note" to become:
      | Topic       | Details                    |
      | parent_note | This is child note example |

  Scenario: Extract the note
    When I extract the note detail
    Then I should see "This is child note example"

  @ignore
  Scenario: There is no note detail
    Given I have a note detail
    When I not select any text
    Then Extract button will be disabled
