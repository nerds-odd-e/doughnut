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
    Then I should see a notification of OpenAI service unavailability in the controller bar

  Scenario: Completing Note Details Using OpenAI
    Given the OpenAI assistant will create a thread and request for the following actions:
      | response              | arguments         |
      | complete note details | " vigorous city." |
    When I request to complete the details for the note "Taipei"
    Then the note details on the current page should be "It is a vigorous city."

  Scenario: Completing Note Details Using OpenAI (new)
    Given OpenAI assistant will reply below for user messages:
      | user message                      | response type   | assistant reply                 | run id |
      | Please complete the note details. | requires action | {"completion": " vigorous city."} | run1   |
    When I request to complete the details for the note "Taipei" new
    Then the note details on the current page should be "It is a vigorous city."
