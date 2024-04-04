Feature: Extract Child Note

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "parent_note"
    Given I update note "parent_note" to become:
      | Topic       | Details                    |
      | parent_note | This is child note example |

  @focus
  Scenario: Extract the note
    When I extract the note detail
    Then I create a note belonging to "parent_note":
      | Topic     | Wikidata Id |
      | Singapore | Q334        |
    And I should see "My Notes/parent_note" with these children
      | Topic      |
      | Singapore  |

  @ignore
  Scenario: There is no note detail
    Given I have a note detail
    When I not select any text
    Then Extract button will be disabled
