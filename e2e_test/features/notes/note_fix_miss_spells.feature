Feature: Note Misspelling Correction
  As a user
  I want to utilize GPT to correct misspellings in my notes
  So that I can ensure accuracy and readability

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | details                          |
      | LeSS in Action  | I have an apple phone             |
      | Deep Learning   | I love deeplearning and AI stuff  |
  @focus
  Scenario: Correct misspellings in a note
    Given I have a note with "misspellings"
    When I ask GPT to correct the misspellings in the note with topic "LeSS in Action"
    Then I should see "I have an Apple phone" in the details of the note with topic "LeSS in Action"

  Scenario: Correct misspellings in a note with complex details
    Given I have a note with "complex details and misspellings"
    When I ask GPT to correct the misspellings in the note with topic "Deep Learning"
    Then the misspellings in the details should be corrected without altering complex structures

  Scenario: Correct misspellings in a note with special characters
    Given I have a note with "misspellings and special characters"
    When I ask GPT to correct the misspellings in the note with special characters
    Then the misspellings in the note should be corrected without affecting the special characters

  Scenario: Handle empty notes gracefully
    Given I have an empty note
    When I ask GPT to correct the misspellings in the note
    Then I should receive a message indicating that the note is empty

  Scenario: Handle notes with correct spelling gracefully
    Given I have a note without any misspellings
    When I ask GPT to correct the misspellings in the note
    Then the note should remain unchanged

  Scenario: Fix miss spell of a note topic "LeSS in Action" with broken srt format
    Given I have a note that has broken SRT format
    When I ask GPT to fix miss spells of note topic "LeSS in Action" with broken SRT format
    Then I should see an error message "SRT format is wrong"

  Scenario: Handle empty input gracefully
    Given I have no notes
    When I ask GPT to correct the misspellings in the notes
    Then The button is disable

  Scenario: Handle server error gracefully
    Given the GPT server is down
    When I ask GPT to correct misspellings in a note
    Then I should receive a message indicating a server error occurred
