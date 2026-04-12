Feature: Change depth of book blocks

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Code Refactoring Book"
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
