Feature: Extract Child Note

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "parent_note"
    Given I update note "parent_note" to become:
      | Topic       | Details                    |
      | parent_note | This is child note example |

  @ignore
  Scenario: Case 1
      When I create a note belonging to "parent_note":
        | Topic     | Wikidata Id |
        | Singapore | Q334        |

    Then I should see "My Notes/parent_note/Singapore" with these children
      | note-topic |
      | Singapore  |

  @ignore
  Scenario: Case 2
    Given I have a note detail
    When I not select any text
    Then Extract button will be disabled
