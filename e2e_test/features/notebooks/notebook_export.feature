@startWithEmptyDownloadsFolder
Feature: Notebook export for Obsidian
  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Root Note" and notes:
      | Title        | Details         | Parent Title |
      | Parent Note  | Parent Content  | Root Note    |
      | Child Note   | Child Content   | Parent Note  |
      | Leaf Note    | Leaf Content    | Parent Note  |

  Scenario: Export notebook as a hierarchical zip file for Obsidian
    When I export notebook "Root Note" to Obsidian markdown zip file
    Then I should receive a zip file containing
      | Filename                                | Format | Content                    |
      | Root Note/__index.md                    | md     | # Root Note\nnull  |
      | Root Note/Parent Note/__index.md        | md     | # Parent Note\nParent Content |
      | Root Note/Parent Note/Child Note.md     | md     | # Child Note\nChild Content |
      | Root Note/Parent Note/Leaf Note.md      | md     | # Leaf Note\nLeaf Content  |
