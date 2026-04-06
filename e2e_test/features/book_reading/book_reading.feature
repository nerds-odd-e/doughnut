@withCliConfig
@interactiveCLI
@mockMineruLib
Feature: Book reading

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"
    And I have a valid Doughnut Access Token with label "for cli"
    And I add the saved access token in the interactive CLI using add-access-token
    When I attach book "refactoring.pdf" to the notebook "Top Maths" via the CLI
    And I open the book attached to notebook "Top Maths"

  Scenario: See book structure and beginning of PDF in the browser
    Then I should see the book structure in the browser:
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
      | 1 | 4.2 Refactoring to Prepare for the Next Change |
      | 1 | 5. Refactoring in Team Development |
      | 1 | 6. Why Refactoring Matters More with AI |
    And I should see the beginning of the PDF book "refactoring.pdf"
    And jumping between outline rows on the same page should scroll the PDF to different positions

  Scenario: Outline row jumps the PDF to the anchored page
    When I choose the book outline row "2.2 Refactoring as Strengthening the Code"
    Then I should see in the book reader visible PDF viewport text including "Strengthening the Code"
    And the book outline row "2.2 Refactoring as Strengthening the Code" should be selected in the book reader

  Scenario: Scrolling the PDF updates the viewport-current outline row
    When I scroll the PDF book reader to bring page 2 into primary view
    Then I should see PDF page 2 marker "Strengthening the Code" in the book reader
    And the book outline row "2.2 Refactoring as Strengthening the Code" should be viewport-current in the book reader

  Scenario: Short viewport scrolls outline aside so viewport-current row stays visible
    When I set the book reading viewport to 1200 by 280
    And I scroll the PDF book reader to bring page 2 into primary view
    Then I should see PDF page 2 marker "Strengthening the Code" in the book reader
    And the book outline row "2.2 Refactoring as Strengthening the Code" should be viewport-current and visible in the outline aside

  Scenario: Same-page scroll moves viewport-current; selected outline row stays the explicit choice
    When I choose the book outline row "1. Refactoring: Protecting Intention in Working Software"
    Then the book outline row "1. Refactoring: Protecting Intention in Working Software" should be selected in the book reader
    And the book outline row "1. Refactoring: Protecting Intention in Working Software" should be viewport-current in the book reader
    When I scroll the PDF book reader down within the same page to move viewport past the next outline bbox
    Then the book outline row "1. Refactoring: Protecting Intention in Working Software" should be selected in the book reader
    And the book outline row "2.1 Easier to Change—and Harder to Misuse" should be viewport-current in the book reader
