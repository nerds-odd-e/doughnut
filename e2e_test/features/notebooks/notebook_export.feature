@startWithEmptyDownloadsFolder
Feature: Notebook export for Obsidian
  Background:
    Given I am logged in as an existing user
    And I have a notebook "Export home" with a note "Index" and notes:
      | Title       | Details        | Folder          |
      | Parent Note | Parent Content | Index           |
      | Child Note  | Child Content  | Index/Parent Note |
      | Leaf Note   | Leaf Content   | Index/Parent Note |

  Scenario: Export notebook as a hierarchical zip file for Obsidian
    When I export notebook "Export home" to Obsidian markdown zip file
    Then I should receive a zip file containing
      | Filename                         | Format | Content                       |
      | index.md                         | md     | # Index\nnull                 |
      | Parent Note/__index.md           | md     | # Parent Note\nParent Content |
      | Parent Note/Child Note.md        | md     | # Child Note\nChild Content   |
      | Parent Note/Leaf Note.md         | md     | # Leaf Note\nLeaf Content     |
