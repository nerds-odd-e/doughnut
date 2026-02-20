@disableOpenAiService
Feature: Confirm Spelling Before Keep For Repetition
  As a learner, when I check 'remember spelling' and click 'keep for repetition',
  I want to verify my spelling first to ensure I actually know the word
  by typing the note title correctly.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review

  Scenario Outline: Verify spelling proceeds with keep for repetition
    And there are some notes:
      | Title        | Details   | Parent Title |
      | <note_title> | <details> | English      |
    And I am assimilating the note "<note_title>"
    And I check the option of remembering spelling
    And I click "Keep for repetition" button
    When I verify spelling with "<spelling_input>"
    Then the note "<note_title>" should be assimilated with remembering spelling

    Examples:
      | note_title     | details                         | spelling_input |
      | sedition       | Sedition means incite violence  | sedition       |
      | colour / color | Colour is the visual perception | colour         |

  Scenario: Show error message when spelling is incorrect
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
    And I am assimilating the note "sedition"
    And I check the option of remembering spelling
    And I click "Keep for repetition" button
    When I verify spelling with "wrong answer"
    Then I should see an error message "wrong spelling" below the input field
    And the popup should remain open
