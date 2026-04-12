Feature: Cancel a book block

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Cancel Block Book"
    When I attach a fake blank pdf book with layout of "subtree_indent" to the notebook "Cancel Block Book"
    And I open the book attached to notebook "Cancel Block Book"

  Scenario: Cancel a leaf block removes it from the layout
    When I choose the book block "Chapter B"
    Then the book block "Chapter B" should be focused in the book layout
    When I press "Backspace" on the book layout
    Then the book block "Chapter B" should no longer appear in the book layout

  Scenario: Cancel a parent block promotes its children
    When I choose the book block "Chapter A"
    Then the book block "Chapter A" should be focused in the book layout
    When I press "Backspace" on the book layout
    Then the book block "Chapter A" should no longer appear in the book layout
    And the book block "A.1 First section" should be at depth 0 in the book layout
    And the book block "A.2 Second section" should be at depth 0 in the book layout
