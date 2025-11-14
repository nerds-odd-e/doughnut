@usingMockedOpenAiService
Feature: Note details completion
  As a user, I want to complete the details of a note using OpenAI so that I can save time and effort in writing the details.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Taiwan" and notes:
      | Title   | Details    | Parent Title |
      | Taipei  | It is a    | Taiwan       |
      | Weather | It rains a | Taipei       |

  # TODO: Re-enable after error handling is properly migrated
  # Scenario: OpenAI Service Unavailability
  #   Given the OpenAI service is unavailable due to invalid system token
  #   When I request to complete the details for the note "Taipei"
  #   Then I should see a notification of a bad request

  Scenario: Completing Note Details Using OpenAI and accepting
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                   |
      | Please complete the note details. | requires action | {"completion": " vigorous city."} |
    And OpenAI can accept tool call results submission and cancellation
    When I request to complete the details for the note "Taipei"
    Then I should see the suggested completion "... vigorous city." in the chat dialog
    When I accept the suggested completion
    Then the note details on the current page should be "It is a vigorous city."

  # TODO: Rejecting tool calls will work after removing old Assistant API endpoints in Step 5
  # Scenario: Completing Note Details Using OpenAI and rejecting
  #   Given OpenAI will reply below for user messages:
  #     | user message                      | response type   | assistant reply                   |
  #     | Please complete the note details. | requires action | {"completion": " vigorous city."} |
  #   When I request to complete the details for the note "Taipei"
  #   Then I should see the suggested completion "... vigorous city." in the chat dialog
  #   When I reject the suggested completion
  #   Then the note details on the current page should be "It is a"
