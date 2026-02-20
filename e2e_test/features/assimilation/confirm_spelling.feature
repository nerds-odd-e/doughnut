@disableOpenAiService
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
    And I am assimilating the note "sedition"
    And I check the option of remembering spelling
    And I click "Keep for repetition" button
    And I should see the spelling verification popup

  Scenario: Verify spelling with correct answer proceeds with keep for repetition
    When I verify spelling with "sedition"
    Then the note "sedition" should be assimilated with remembering spelling

  Scenario: Show error message when spelling is incorrect
    When I verify spelling with "wrong answer"
    Then I should see an error message "wrong spelling" below the input field
    And the popup should remain open

  Scenario: Verify spelling accepts partial answers for notes with slash format
    Given there are some notes:
      | Title                      | Details                        | Parent Title |
      | rebellion / insurrection   | Rebellion means armed resistance | English      |
    And I am assimilating the note "rebellion / insurrection"
    And I check the option of remembering spelling
    And I click "Keep for repetition" button
    And I should see the spelling verification popup
    When I verify spelling with "rebellion"
    Then the note "rebellion / insurrection" should be assimilated with remembering spelling
