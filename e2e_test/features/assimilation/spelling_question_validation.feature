@disableOpenAiService @ignore
Feature: Spelling Question Validation
  As a learner, I want to be prevented from enabling spelling questions
  for notes without details, so that the spelling quiz can work properly.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Learning" which skips review

  Scenario: Cannot enable spelling question for note without details
    Given there are some notes:
      | Title | Details | Parent Title |
      | Word  |         | Learning     |
    When I navigate to the assimilation page for note "Word"
    And I check the "Remember Spelling" option
    Then I should see an error "Remember spelling note need to have detail" on "Remember Spelling" field