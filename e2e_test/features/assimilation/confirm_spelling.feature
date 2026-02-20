@disableOpenAiService
Feature: Confirm Spelling Before Keep For Repetition
  As a learner, when I keep for repetition with remembering spelling,
  I want to verify my spelling first to ensure I actually know the word
  by typing the note title correctly.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review

  Scenario Outline: Verify spelling proceeds with keep for repetition
    Given there are some notes:
      | Title        | Details           | Parent Title |
      | <note_title> | Non-empty details | English      |
    And I am assimilating the note "<note_title>"
    And I keep for repetition with remembering spelling
    When I verify spelling with "<spelling_input>"
    Then the note "<note_title>" should be assimilated with remembering spelling

    Examples:
      | note_title     | spelling_input |
      | sedition       | sedition       |
      | colour / color | colour         |

  Scenario: Show error message when spelling is incorrect
    Given there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
    And I am assimilating the note "sedition"
    And I keep for repetition with remembering spelling
    When I verify spelling with "wrong answer"
    Then I should see an error message "wrong spelling" in the spelling verification popup
