Feature: Change depth of book blocks

  Background:
    Given I am logged in as an existing user

  Rule: Change depth with keyboard

    Background:
      Given I have a notebook with the head note "Code Refactoring Book"
      When I attach a fake blank pdf book with layout of "refactoring" to the notebook "Code Refactoring Book"
      And I open the book attached to notebook "Code Refactoring Book"

    Scenario Outline: Change depth with keyboard
      Given the book layout shows block "<block>" at depth <start_depth>
      When I choose the book block "<block>"
      Then the book block "<block>" should be focused in the book layout
      When I press "<key>" on the book layout
      Then the book block "<block>" should be at depth <end_depth> in the book layout

      Examples:
        | block                                   | start_depth | key       | end_depth |
        | 2. The Usual Defi nition Is Not Enough  | 0           | Tab       | 1         |
        | 3.1 Can You Refactor Without Tests?     | 1           | Shift+Tab | 0         |

  Rule: Change depth of a block with its descendants

    Background:
      Given I have a notebook with the head note "Subtree Indent Book"
      When I attach a fake blank pdf book with layout of "subtree_indent" to the notebook "Subtree Indent Book"
      And I open the book attached to notebook "Subtree Indent Book"

    Scenario: Indent a block and its children together
      Given the book layout shows block "Chapter A" at depth 0
      When I choose the book block "Chapter A"
      Then the book block "Chapter A" should be focused in the book layout
      When I press "Tab" on the book layout
      Then the book block "Chapter A" should be at depth 1 in the book layout
      And the book block "A.1 First section" should be at depth 2 in the book layout
      And the book block "A.2 Second section" should be at depth 2 in the book layout
      And the book block "Chapter B" should be at depth 0 in the book layout
