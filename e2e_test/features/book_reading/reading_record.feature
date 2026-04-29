Feature: Reading record

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Refactoring read" with a note "Code Refactoring Book"
    When I attach a fake blank pdf book with layout of "refactoring" to the notebook "Code Refactoring Book"
    And I open the book attached to notebook "Refactoring read"

  Rule: From section 1, scroll within page past the next book block bbox

    Background:
      When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
      And I scroll the PDF book reader down within the same page to move viewport past the next book block bbox

    Scenario: Auto-read a heading-only book block when entering its successor (reading record)
      Then the book block "2.1 Easier to Change—and Harder to Misuse" should be the current block in the book reader
      And I should see that book block "2. The Usual Defi nition Is Not Enough" is marked as read in the book layout

    Scenario: Current block differs from selected shows navigation affordance (reading record)
      Then I should see the current block navigation bar showing "2.1 Easier to Change—and Harder to Misuse"
      When I click "Read from here" in the current block navigation bar
      Then I should see that book block "2.1 Easier to Change—and Harder to Misuse" is selected in the book layout
      And the current block navigation bar should not be visible

    Scenario: Back to selected scrolls back to the selected block (reading record)
      Then I should see the current block navigation bar showing "2.1 Easier to Change—and Harder to Misuse"
      When I click "Back to selected" in the current block navigation bar
      Then the book block "1. Refactoring: Protecting Intention in Working Software" should be the current block in the book reader
      And the book block "1. Refactoring: Protecting Intention in Working Software" should be the current selection in the book reader

  Scenario: Mark a book block as read (reading record)
    When I choose the book block "2.1 Easier to Change—and Harder to Misuse"
    And I scroll the PDF book reader until the Reading Control Panel shows for "2.1 Easier to Change—and Harder to Misuse"
    And I mark the book block "2.1 Easier to Change—and Harder to Misuse" as read in the Reading Control Panel
    Then I should see that book block "2.1 Easier to Change—and Harder to Misuse" is marked as read in the book layout
    And I should see that book block "2.2 Refactoring as Strengthening the Code" is selected in the book layout

  Scenario: Panel auto-targets next block when selected is already marked (reading record)
    When I choose the book block "3.1 Can You Refactor Without Tests?"
    And I scroll the PDF book reader until the Reading Control Panel shows for "3.1 Can You Refactor Without Tests?"
    And I mark the book block "3.1 Can You Refactor Without Tests?" as read in the Reading Control Panel
    Then I should see that book block "3.2 Can You Refactor Without Changing the Code?" is selected in the book layout
    When I choose the book block "3.1 Can You Refactor Without Tests?"
    And I scroll the PDF book reader until the Reading Control Panel shows for "3.2 Can You Refactor Without Changing the Code?"
    And I mark the book block "3.2 Can You Refactor Without Changing the Code?" as read in the Reading Control Panel
    Then I should see that book block "3.2 Can You Refactor Without Changing the Code?" is marked as read in the book layout

  Scenario: Mark the last book block as read (reading record)
    When I choose the book block "6. Why Refactoring Matters More with AI"
    And I scroll the PDF book reader until the Reading Control Panel shows for "6. Why Refactoring Matters More with AI"
    And I mark the book block "6. Why Refactoring Matters More with AI" as read in the Reading Control Panel
    Then I should see that book block "6. Why Refactoring Matters More with AI" is marked as read in the book layout
