@usingMockedOpenAiService
Feature: Note details completion
  As a user, I want to complete the details of a note using OpenAI so that I can save time and effort in writing the details.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Taiwan" and notes:
      | Topic   | Details    | Parent Topic |
      | Taipei  | It is a    | Taiwan       |
      | Weather | It rains a | Taipei       |

  Scenario: OpenAI Service Unavailability
    Given the OpenAI service is unavailable due to invalid system token
    When I request to complete the details for the note "Taipei"
    Then I should see a notification of a bad request

  Scenario Outline: Completing Note Details Using OpenAI
    Given OpenAI assistant will reply below for user messages:
      | user message                      | response type   | assistant reply                 | run id |
      | Please complete the note details. | requires action | {"completion": " vigorous city."} | run1   |
    And OpenAI assistant can accept tool call results submission and run cancellation
    When I request to complete the details for the note "Taipei"
    Then I should see the suggested completion "... vigorous city." in the chat dialog
    When I <action> the suggested completion
    Then the note details on the current page should be "<expected_details>"

    Examples:
      | action | expected_details              |
      | accept | It is a vigorous city.       |
      | reject | It is a                      |
