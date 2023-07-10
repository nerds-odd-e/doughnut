Feature: Note deletion

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | title          | testingParent  |
      | LeSS in Action |                |
      | team           | LeSS in Action |
      | tech           | LeSS in Action |
      | TDD            | tech           |
      | CI System      | tech           |

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
    When I undo "delete note"
    And I should see "My Notes/LeSS in Action/tech" with these children
      | note-title   |
      | CI System    |
    When I undo "delete note" again
    And I should see "My Notes/LeSS in Action/tech" with these children
      | note-title   |
      | CI System    |
      | TDD          |
