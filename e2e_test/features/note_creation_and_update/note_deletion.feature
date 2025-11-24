Feature: Note deletion

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Title            | Parent Title   |
      | team             | LeSS in Action |
      | tech             | LeSS in Action |
      | TDD              | tech           |
      | CI System        | tech           |

  Scenario: Delete a note
    When I delete note "TDD"
    Then I should see the note "TDD" is marked as deleted

  @ignore
  Scenario: Delete a note will delete its links
    Given there is "a part of" link between note "TDD" and "tech"
    When I delete note "TDD"
    Then I should see "tech" has no link to "TDD"
    When I undo "delete note"
    Then I should see "tech" has link "a par of" to "TDD"

  Scenario: Delete a note then delete its parent and undo
    Given I delete note "TDD" at 13:00
    And I delete note "tech" at 14:00
    When I undo delete note to recover note "tech"
    And I should see "LeSS in Action/tech" with these children
      | note-title |
      | CI System  |
    When I undo delete note to recover note "TDD" again
    And I should see "LeSS in Action/tech" with these children
      | note-title |
      | CI System  |
      | TDD        |
