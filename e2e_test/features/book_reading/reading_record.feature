Feature: Reading record

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"
    When I attach a book with MinerU fixture "refactoring" to the notebook "Top Maths"
    And I open the book attached to notebook "Top Maths"

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
