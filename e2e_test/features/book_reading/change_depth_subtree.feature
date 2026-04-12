Feature: Change depth of a block with its descendants

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Subtree Indent Book"
    When I attach a fake blank pdf book with layout of "subtree_indent" to the notebook "Subtree Indent Book"
    And I open the book attached to notebook "Subtree Indent Book"

  @ignore
  Scenario: Indent a block and its children together
    Given the book layout shows block "Chapter A" at depth 0
    When I choose the book block "Chapter A"
    Then the book block "Chapter A" should be focused in the book layout
    When I press "Tab" on the book layout
    Then the book block "Chapter A" should be at depth 1 in the book layout
    And the book block "A.1 First section" should be at depth 2 in the book layout
    And the book block "A.2 Second section" should be at depth 2 in the book layout
    And the book block "Chapter B" should be at depth 0 in the book layout
