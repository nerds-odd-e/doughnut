@usingMockedOpenAiService
Feature: Chat about a note with AI
  Learner wants to chat with the AI about a certain note,
  so that they can understand the note better.


  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "There are 42 prefectures in Japan"
    And I start to chat about the note "There are 42 prefectures in Japan"


  Scenario: The users can continue to conmunication with AI
    Given OpenAI assistant will reply below for user messages:
      | user message          | list after message id | assistant reply              | message id   |
      | Is Naba one of them?  |                       | No. It is not.               | message_id_1 |
      | Is this note correct? | message_id_1          | No, there are 47 prefectures | message_id_2 |
    When I send the message "Is Naba one of them?" to AI
    Then I should receive the following chat messages:
      | role      | message              |
      | user      | Is Naba one of them? |
      | assistant | No. It is not.       |
    # When I send the message "Is this note correct?" to AI
    # Then I should receive the following chat messages:
    #   | role      | message                      |
    #   | user      | Is Naba one of them?         |
    #   | assistant | No. It is not.               |
    #   | user      | Is this note correct?        |
    #   | assistant | No, there are 47 prefectures |
