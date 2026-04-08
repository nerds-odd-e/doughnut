@withCliConfig
@interactiveCLI
@mockMineruLib
Feature: Reading record

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"
    And I set the access token for "old_learner" in the interactive CLI
    When I attach book "refactoring.pdf" to the notebook "Top Maths" via the CLI
    And I open the book attached to notebook "Top Maths"

  Scenario: Mark a book range as read (reading record)
    When I choose the book range "2.1 Easier to Change—and Harder to Misuse"
    And I scroll the PDF until the book range "2.2 Refactoring as Strengthening the Code" is the current range in the book reader
    When I mark the book range "2.1 Easier to Change—and Harder to Misuse" as read in the Reading Control Panel
    Then I should see that book range "2.1 Easier to Change—and Harder to Misuse" is marked as read in the book layout
    # And I should see that book range "2.2 Refactoring as Strengthening the Code" is selected in the book layout
