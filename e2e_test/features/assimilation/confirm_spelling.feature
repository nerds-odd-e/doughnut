@ignore
Feature: Confirm Spelling Before Keep For Repetition
  As a learner, when I check 'remember spelling' and click 'keep for repetition',
  I want to verify my spelling first to ensure I actually know the word
  by typing the note title correctly.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |

  Scenario: Cancel popup and reopen it
    Given I am assimilating the note "sedition"
    And I check the option of remembering spelling
    And I click "Keep For Repetition" button
    And I should see the spelling verification popup
    When I click "Cancel" button on the popup
    Then the popup should be closed
    And I should still be on the assimilate page for "sedition"
    When I click "Keep For Repetition" button
    Then I should see the spelling verification popup

  Scenario: Verify spelling with correct answer proceeds with keep for repetition
    Given I am assimilating the note "sedition"
    And I check the option of remembering spelling
    And I click "Keep For Repetition" button
    And I should see the spelling verification popup
    When I type "sedition" in the verification input
    And I click "Verify" button on the popup
    Then the note "sedition" should be assimilated with remembering spelling

  Scenario: Show error message when spelling is incorrect
    Given I am assimilating the note "sedition"
    And I check the option of remembering spelling
    And I click "Keep For Repetition" button
    And I should see the spelling verification popup
    When I type "wrong answer" in the verification input
    And I click "Verify" button on the popup
    Then I should see an error message "wrong spelling" below the input field
    And the popup should remain open
