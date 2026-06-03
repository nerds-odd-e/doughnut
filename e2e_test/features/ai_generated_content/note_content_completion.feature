@usingMockedOpenAiService
Feature: Note content completion
  As a user, I want to complete the content of a note using OpenAI so that I can save time and effort in writing the content.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Taiwan primer" with notes:
      | Title  | Content    |
      | Taipei | It is a    |

  Scenario: OpenAI Service Unavailability
    Given the OpenAI service is unavailable due to invalid system token
    When I request to complete the content for the note "Taipei"
    Then I should see a notification of a bad request

  Scenario: Completing Note Content Using OpenAI and accepting
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                                                      |
      | Please complete the note content. | requires action | {"content": "It is a vigorous city."} |
    When I request to complete the content for the note "Taipei"
    Then I should see the suggested completion in the chat dialog
    When I accept the suggested completion
    Then the note content on the current page should be "It is a vigorous city."

  Scenario: Completing Note Content Using OpenAI and rejecting
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                                                      |
      | Please complete the note content. | requires action | {"content": "It is a vigorous city."} |
    When I request to complete the content for the note "Taipei"
    Then I should see the suggested completion in the chat dialog
    When I reject the suggested completion
    Then the note content on the current page should be "It is a"
