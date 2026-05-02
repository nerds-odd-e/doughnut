@usingMockedOpenAiService
Feature: Note details completion
  As a user, I want to complete the details of a note using OpenAI so that I can save time and effort in writing the details.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Taiwan primer" with a note "Taiwan" and notes:
      | Title   | Folder       | Details    |
      | Taipei  | Taiwan       | It is a    |
      | Weather | Taiwan/Taipei | It rains a |

  Scenario: OpenAI Service Unavailability
    Given the OpenAI service is unavailable due to invalid system token
    When I request to complete the details for the note "Taipei"
    Then I should see a notification of a bad request

  Scenario: Completing Note Details Using OpenAI and accepting
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                                                      |
      | Please complete the note details. | requires action | {"details": "It is a vigorous city."} |
    When I request to complete the details for the note "Taipei"
    Then I should see the suggested completion in the chat dialog
    When I accept the suggested completion
    Then the note details on the current page should be "It is a vigorous city."

  Scenario: Completing Note Details Using OpenAI and rejecting
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                                                      |
      | Please complete the note details. | requires action | {"details": "It is a vigorous city."} |
    When I request to complete the details for the note "Taipei"
    Then I should see the suggested completion in the chat dialog
    When I reject the suggested completion
    Then the note details on the current page should be "It is a"
