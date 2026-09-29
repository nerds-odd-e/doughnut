Feature: Book browsing

  Background:
    Given I am logged in as an existing user

  Rule: A PDF without bookmarks is laid out by its MinerU headings

    Background:
      Given I have a notebook "Refactoring read"
      And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
      And I open the book attached to notebook "Refactoring read"

    Scenario: See book layout and beginning of PDF in the browser
      Then I should see the book layout in the browser:
        | 0 | Code Refactoring |
        | 0 | 1. Refactoring: Protecting Intention in Working Software |
        | 1 | 3.1 Can You Refactor Without Tests? |
      And I should see the beginning of the PDF book "refactoring.pdf"

    Scenario: Book block jumps the PDF to the anchored page
      When I choose the book block "2.2 Refactoring as Strengthening the Code"
      Then the book reader PDF viewport should be on page 2
      And the book block "2.2 Refactoring as Strengthening the Code" should be the current selection in the book reader

    Scenario: Scrolling the PDF updates the current block
      When I scroll the PDF book reader to bring page 2 into primary view
      Then the book reader PDF viewport should be on page 2
      And the book block "2.2 Refactoring as Strengthening the Code" should be the current block in the book reader

    Scenario: Short viewport keeps the book layout aside scrolled to the current block
      When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
      And I set the book reading viewport to 1200 by 280
      And I scroll the PDF book reader to bring page 2 into primary view
      Then the book reader PDF viewport should be on page 2
      And the book block "2.2 Refactoring as Strengthening the Code" should be the current block and visible in the book layout aside

    Scenario: Same-page scroll moves the current block; the current selection stays the explicit choice
      When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
      Then the book block "1. Refactoring: Protecting Intention in Working Software" should be the current selection in the book reader
      When I scroll the PDF book reader down within the same page to move viewport past the next book block bbox
      Then the book reader PDF viewport should be on page 1
      And the book block "1. Refactoring: Protecting Intention in Working Software" should be the current selection in the book reader
      And the book block "2.1 Easier to Change—and Harder to Misuse" should be the current block in the book reader

  Rule: A PDF with bookmarks is laid out by its bookmarks

    Background:
      Given I have a notebook "Refactoring bookmarks"
      And I attach the pdf book "refactoring.pdf" with its MinerU output to the notebook "Refactoring bookmarks"
      And I open the book attached to notebook "Refactoring bookmarks"

    Scenario: Bookmark blocks show, land where they point, and follow scrolling
      Then I should see the book layout in the browser:
        | 0 | *beginning* |
        | 0 | 1. Refactoring: Protecting Intention in Working Software |
        | 0 | 2. The Usual Definition Is Not Enough |
        | 0 | 3. Refactoring Is Not Only About Changing Production Code |
        | 0 | 4. Two Different Kinds of Refactoring |
        | 0 | 5. Refactoring in Team Development |
        | 0 | 6. Why Refactoring Matters More with AI |
      When I choose the book block "4. Two Different Kinds of Refactoring"
      Then the book reader PDF viewport should be on page 4
      And the book block "4. Two Different Kinds of Refactoring" should be the current selection in the book reader
      When I scroll the PDF book reader to the top of page 5
      Then the book reader PDF viewport should be on page 5
      And the book block "5. Refactoring in Team Development" should be the current block in the book reader
