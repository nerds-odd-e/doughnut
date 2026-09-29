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
    When I scroll the PDF book reader until the Reading Control Panel shows for "Code Refactoring"
    Then the Reading Control Panel should be fully on the screen
    When I mark the book block "Code Refactoring" as read in the Reading Control Panel
    Then I should see that book block "Code Refactoring" is marked as read in the book layout

  Scenario: Open the book layout on a phone
    Given I have a notebook "Refactoring read"
    And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
    And I open the book attached to notebook "Refactoring read"
    When I tap the Book layout toggle
    Then the book layout should be open

  Scenario: Reopen the book layout on a tablet
    Given I am on a window 768 * 1024
    And I have a notebook "Refactoring read"
    And I attach a fake blank pdf book with book layout of "refactoring" to the notebook "Refactoring read"
    And I open the book attached to notebook "Refactoring read"
    When I tap the Book layout toggle
    Then the book layout should be closed
    When I tap the Book layout toggle
    Then the book layout should be open
