@withCliConfig
@interactiveCLI
@mockMineruLib
Feature: Book browsing

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"
    And I set the access token for "old_learner" in the interactive CLI
    When I attach book "refactoring.pdf" to the notebook "Top Maths" via the CLI
    And I open the book attached to notebook "Top Maths"

  Scenario: See book layout and beginning of PDF in the browser
    Then I should see the book layout in the browser:
      | 0 | Code Refactoring |
      | 0 | 1. Refactoring: Protecting Intention in Working Software |
      | 0 | 2. The Usual Defi nition Is Not Enough |
      | 0 | 2.1 Easier to Change—and Harder to Misuse |
      | 0 | 2.2 Refactoring as Strengthening the Code |
      | 0 | 3. Refactoring Is Not Only About Changing Production Code |
      | 1 | 3.1 Can You Refactor Without Tests? |
      | 1 | 3.2 Can You Refactor Without Changing the Code? |
      | 1 | 4. Two Diff erent Kinds of Refactoring |
      | 1 | 4.1 Refactoring to Protect Current Intent |
      | 1 | 4.1.1 Refactoring Art |
      | 1 | 4.2 Refactoring to Prepare for the Next Change |
      | 1 | 5. Refactoring in Team Development |
      | 1 | 6. Why Refactoring Matters More with AI |
    And I should see the beginning of the PDF book "refactoring.pdf"
    When I choose the book block "2. The Usual Defi nition Is Not Enough"
    Then I should see in the book reader visible PDF viewport on page 1 text including "Usual Definition"

  Scenario: Book block jumps the PDF to the anchored page
    When I choose the book block "2.2 Refactoring as Strengthening the Code"
    Then I should see in the book reader visible PDF viewport on page 2 text including "Strengthening the Code"
    And the book block "2.2 Refactoring as Strengthening the Code" should be the current selection in the book reader

  Scenario: Scrolling the PDF updates the current block; short viewport keeps aside scrolled to it
    When I scroll the PDF book reader to bring page 2 into primary view
    Then I should see in the book reader visible PDF viewport on page 2 text including "Strengthening the Code"
    And the book block "2.2 Refactoring as Strengthening the Code" should be the current block in the book reader
    When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
    And I set the book reading viewport to 1200 by 280
    When I scroll the PDF book reader to bring page 2 into primary view
    Then I should see in the book reader visible PDF viewport on page 2 text including "Strengthening the Code"
    And the book block "2.2 Refactoring as Strengthening the Code" should be the current block and visible in the book layout aside

  Scenario: Same-page scroll moves the current block; the current selection stays the explicit choice
    When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
    Then the book block "1. Refactoring: Protecting Intention in Working Software" should be the current selection in the book reader
    And the book block "1. Refactoring: Protecting Intention in Working Software" should be the current block in the book reader
    When I scroll the PDF book reader down within the same page to move viewport past the next book block bbox
    Then I should see in the book reader visible PDF viewport on page 1 text including "Easier to Change"
    And the book block "1. Refactoring: Protecting Intention in Working Software" should be the current selection in the book reader
    And the book block "2.1 Easier to Change—and Harder to Misuse" should be the current block in the book reader
