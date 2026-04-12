Feature: Change depth of book blocks

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Code Refactoring Book"
    When I attach a fake blank pdf book with layout of "refactoring" to the notebook "Code Refactoring Book"
    And I open the book attached to notebook "Code Refactoring Book"

  Scenario: Focus a book block in the layout
    When I choose the book block "2. The Usual Defi nition Is Not Enough"
    Then the book block "2. The Usual Defi nition Is Not Enough" should be focused in the book layout

  Scenario: Indent a leaf block in the book layout
    Given the book layout shows block "2. The Usual Defi nition Is Not Enough" at depth 0
    When I choose the book block "2. The Usual Defi nition Is Not Enough"
    And I press Tab on the book layout
    Then the book block "2. The Usual Defi nition Is Not Enough" should be at depth 1 in the book layout
