@usingMockedOpenAiService
Feature: Conversation about a note with AI
  As a learner, I want to start a conversation with AI about a note,
  so that I can understand the note better.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Japan facts" with a note "There are 42 prefectures in Japan"
    And OpenAI will reply below for user messages:
      | user message          | assistant reply              |
      | Is Naba one of them?  | No. It is not.               |
      | Is this note correct? | No, there are 47 prefectures |

  Scenario: Ask AI about a note, follow up, and export the conversation
    When I start a conversation about the note "There are 42 prefectures in Japan" inviting AI with the message "Is Naba one of them?"
    Then I should see the following messages:
      | role      | message              |
      | user      | Is Naba one of them? |
      | assistant | No. It is not.       |
    And OpenAI responses were called with Doughnut focus context
    When I send the message "Is this note correct?" to AI
    Then I should see the following messages:
      | role      | message                      |
      | user      | Is this note correct?        |
      | assistant | No, there are 47 prefectures |
    When I export the conversation
    Then the export should contain the user message "Is Naba one of them?"
    And the export should contain the assistant reply "No. It is not."
    And I should be able to copy the export
