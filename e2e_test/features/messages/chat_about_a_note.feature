@usingMockedOpenAiService
Feature: Chat about a note with AI
  Learner wants to chat with the AI about a certain note,
  so that they can understand the note better.


  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "There are 42 prefectures in Japan"


  Scenario: The users can chat with AI about the current note
    Given OpenAI will reply below for user messages:
      | user message          | assistant reply              |
      | Is Naba one of them?  | No. It is not.               |
      | Is this note correct? | No, there are 47 prefectures |
    When I start a conversation about the note "There are 42 prefectures in Japan" with a message "Is Naba one of them?" to AI
    Then I should receive the following chat messages:
      | role      | message              |
      | user      | Is Naba one of them? |
      | assistant | No. It is not.       |
    When I send the message "Is this note correct?" to AI
    Then I should receive the following chat messages:
      | role      | message                      |
      | user      | Is Naba one of them?         |
      | assistant | No. It is not.               |
      | user      | Is this note correct?        |
      | assistant | No, there are 47 prefectures |


  Scenario: User can export conversation to continue in external AI tools
    Given OpenAI will reply below for user messages:
      | user message         | assistant reply |
      | Is Naba one of them? | No. It is not.  |
    When I start a conversation about the note "There are 42 prefectures in Japan" with a message "Is Naba one of them?" to AI
    Then I should receive the following chat messages:
      | role      | message              |
      | user      | Is Naba one of them? |
      | assistant | No. It is not.       |
    When I export the conversation
    Then the export should contain the user message "Is Naba one of them?"
    And the export should contain the assistant reply "No. It is not."
    And I should be able to copy the export
