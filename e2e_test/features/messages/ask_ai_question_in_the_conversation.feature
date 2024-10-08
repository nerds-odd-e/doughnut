@usingMockedOpenAiService
Feature: Ask AI question in the conversation
  User wants to ask question to AI in a conversation,
  so that they can get AI's opinion.


  # prepare data
  Background:
    Given I am logged in as an existing user
    # prepare data
    And I have a notebook with the head note "There are 42 prefectures in Japan"

  @ignore
  Scenario: The users can ask question to AI in the conversation
    Given OpenAI assistant will reply below for user messages:
      | user message          | assistant reply              | run id |
      | Is Naba one of them?  | No. It is not.               | run1   |
      | Is this note correct? | No, there are 47 prefectures | run2   |
    When I start to chat about the note "There are 42 prefectures in Japan"
    And I send the message "Is Naba one of them?" to AI
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