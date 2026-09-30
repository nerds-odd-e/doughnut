Feature: Reading a book on a phone

  Background:
    Given I am logged in as an existing user
    And I am on a window 390 * 844

  Scenario: A PDF page is as wide as the phone screen
    Given I have a notebook "Refactoring read"
    And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
    When I open the book attached to notebook "Refactoring read"
    Then the PDF pages should use the screen width

  Scenario: EPUB text fills the phone screen width
    Given I have a notebook "EPUB smoke" with a note "EPUB E2E Notebook"
    And I open the notebook settings for "EPUB smoke"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    Then I should see the text "Opening paragraph for part one." in the EPUB reader
    And the EPUB text should use the screen width

  Scenario: Mark a book block as read from the phone screen
    Given I have a notebook "Refactoring read"
    And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
    And I open the book attached to notebook "Refactoring read"
    When I choose the book block "2.1 Easier to Change—and Harder to Misuse"
    And I scroll the PDF book reader until the Reading Control Panel shows for "2.1 Easier to Change—and Harder to Misuse"
    Then the Reading Control Panel should be fully on the screen
    When I mark the book block "2.1 Easier to Change—and Harder to Misuse" as read in the Reading Control Panel
    Then I should see that book block "2.1 Easier to Change—and Harder to Misuse" is marked as read in the book layout

  Scenario: Open the book layout on a phone
    Given I have a notebook "Refactoring read"
    And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
    And I open the book attached to notebook "Refactoring read"
    When I tap the Book layout toggle
    Then the book layout should be open
    And the book layout should be below the main menu

  Scenario: Choosing a book block closes the book layout and moves the book there
    Given I have a notebook "Refactoring read"
    And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
    And I open the book attached to notebook "Refactoring read"
    When I choose the book block "2.2 Refactoring as Strengthening the Code"
    Then the book layout should be closed
    And the top of the PDF book reader should be at 89 of 1000 down page 2
    And the book block "2.2 Refactoring as Strengthening the Code" should be the current selection in the book reader

  Scenario: Tapping outside the book layout closes it and keeps the place
    Given I have a notebook "Refactoring read"
    And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
    And I open the book attached to notebook "Refactoring read"
    When I tap the Book layout toggle
    And I tap outside the book layout
    Then the book layout should be closed
    And the book reader PDF viewport should be on page 1

  Scenario: Reopen the book layout on a tablet
    Given I am on a window 768 * 1024
    And I have a notebook "Refactoring read"
    And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
    And I open the book attached to notebook "Refactoring read"
    When I tap the Book layout toggle
    Then the book layout should be closed
    When I tap the Book layout toggle
    Then the book layout should be open
