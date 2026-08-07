@usingMockedOpenAiService
Feature: Chat about a note with AI
  As a learner, I want to chat with AI about a note,
  so that I can understand the note better.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Japan facts" with a note "There are 42 prefectures in Japan"

  Scenario: Asking AI about a note returns a reply with focus context
    Given OpenAI will reply below for user messages:
      | user message         | assistant reply |
      | Is Naba one of them? | No. It is not.  |
    When I ask AI about the note "There are 42 prefectures in Japan" with the message "Is Naba one of them?"
    Then I should see the following chat messages:
      | role      | message              |
      | user      | Is Naba one of them? |
      | assistant | No. It is not.       |
    And OpenAI responses were called with Doughnut focus context

  Scenario: Follow-up question continues the AI conversation
    Given OpenAI will reply below for user messages:
      | user message          | assistant reply              |
      | Is Naba one of them?  | No. It is not.               |
      | Is this note correct? | No, there are 47 prefectures |
    When I ask AI about the note "There are 42 prefectures in Japan" with the message "Is Naba one of them?"
    And I send the message "Is this note correct?" to AI
    Then I should see the following chat messages:
      | role      | message                      |
      | user      | Is Naba one of them?         |
      | assistant | No. It is not.               |
      | user      | Is this note correct?        |
      | assistant | No, there are 47 prefectures |

  Scenario: Exporting an AI conversation includes the chat for external tools
    Given OpenAI will reply below for user messages:
      | user message         | assistant reply |
      | Is Naba one of them? | No. It is not.  |
    When I ask AI about the note "There are 42 prefectures in Japan" with the message "Is Naba one of them?"
    Then I should see the following chat messages:
      | role      | message              |
      | user      | Is Naba one of them? |
      | assistant | No. It is not.       |
    When I export the conversation
    Then the export should contain the user message "Is Naba one of them?"
    And the export should contain the assistant reply "No. It is not."
    And I should be able to copy the export
