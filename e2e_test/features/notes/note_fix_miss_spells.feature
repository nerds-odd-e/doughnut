@ignore
Feature: Note Fix Miss Spells
  As a learner, I want to fix miss spells with GPT,
  Keep srt format

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | details              |
      | LeSS in Action   | I have an apple phone |

  Scenario: Fix miss spell of a note
    Given I have a note that has miss spells
    When I ask GPT to fix miss spells of note topic "LeSS in Action":
      | Topic          | Details               |
      | LeSS in Action | I have an Apple phone |
    Then I should see "I have an Apple phone" in topic "LeSS in Action"

  Scenario: Fix miss spell of a note topic "LeSS in Action" with broken srt format
    Given I have a note that has broken SRT format
    When I ask GPT to fix miss spells of note topic "LeSS in Action" with broken SRT format
    Then I should see an error message "SRT format is wrong"
