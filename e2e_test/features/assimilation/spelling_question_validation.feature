@disableOpenAiService
Feature: Spelling Question Validation
  As a learner, I want to be prevented from enabling spelling questions
  for notes without details, so that the spelling quiz can work properly.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Learning" which skips review

  Scenario Outline: Remembering spelling availability depends on note details
    Given there are some notes:
      | Title | Details        | Parent Title |
      | Word  | <details>     | Learning     |
    When I am assimilating the note "Word"
    Then remembering spelling should be <availability>

    Examples:
      | details                | availability |
      |                        | unavailable  |
      | This is the definition | available    |
