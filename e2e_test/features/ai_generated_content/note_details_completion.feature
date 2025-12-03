@usingMockedOpenAiService
Feature: Note details completion
  As a user, I want to complete the details of a note using OpenAI so that I can save time and effort in writing the details.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Taiwan" and notes:
      | Title   | Details    | Parent Title |
      | Taipei  | It is a    | Taiwan       |
      | Weather | It rains a | Taipei       |

  Scenario: OpenAI Service Unavailability
    Given the OpenAI service is unavailable due to invalid system token
    When I request to complete the details for the note "Taipei"
    Then I should see a notification of a bad request

  Scenario: Completing Note Details Using OpenAI and accepting
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                                                                                                 |
      | Please complete the note details. | requires action | {"patch": "--- a\\n+++ b\\n@@ -1,1 +1,1 @@\\n-It is a\\n+It is a vigorous city."} |
    When I request to complete the details for the note "Taipei"
    Then I should see the suggested completion in the chat dialog
    When I accept the suggested completion
    Then the note details on the current page should be "It is a vigorous city."

  Scenario: Completing Note Details Using OpenAI and rejecting
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                                                                                                 |
      | Please complete the note details. | requires action | {"patch": "--- a\\n+++ b\\n@@ -1,1 +1,1 @@\\n-It is a\\n+It is a vigorous city."} |
    When I request to complete the details for the note "Taipei"
    Then I should see the suggested completion in the chat dialog
    When I reject the suggested completion
    Then the note details on the current page should be "It is a"
