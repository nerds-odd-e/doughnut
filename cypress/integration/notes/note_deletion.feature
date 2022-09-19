Feature: Note deletion

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | testingParent  |
      | LeSS in Action |                |
      | team           | LeSS in Action |
      | tech           | LeSS in Action |
      | TDD            | tech           |
      | CI System      | tech           |

  Scenario: Delete a note then delete its parent and undo
    Given I delete note "TDD"
    And I delete note "tech"
    When I undo "delete note"
    And I should see "My Notes/LeSS in Action/tech" with these children
      | note-title   |
      | CI System    |
    When I undo "delete note"
    And I should see "My Notes/LeSS in Action/tech" with these children
      | note-title   |
      | CI System    |
      | TDD          |
