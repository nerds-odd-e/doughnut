@disableOpenAiService
Feature: Assimilate With Remembering Spelling
  As a learner, I want to keep notes for repetition with spelling verification.
  Spelling is only available for notes with details.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips memory tracking

  Scenario Outline: Remembering spelling availability depends on note details
    Given there are some notes:
      | Title | Details        | Parent Title |
      | Word  | <details>      | English      |
    When I am assimilating the note "Word"
    Then remembering spelling should be <availability>

    Examples:
      | case                     | details                 | availability |
      | note has no details      |                         | unavailable  |
      | note has definition      | Definition content      | available    |

  Scenario Outline: Verify spelling proceeds with keep for repetition
    Given there are some notes:
      | Title        | Details           | Parent Title |
      | <note_title> | Non-empty details | English      |
    And I am assimilating the note "<note_title>"
    And I keep for repetition with remembering spelling
    When I verify spelling with "<spelling_input>"
    Then the spelling verification result for note "<note_title>" should be <expected_result>

    Examples:
      | note_title     | spelling_input | expected_result         |
      | sedition       | sedition       | "success"               |
      | colour / color | colour         | "success"               |
      | sedition       | wrong answer   | "error: wrong spelling" |
