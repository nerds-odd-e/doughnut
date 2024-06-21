@usingMockedOpenAiService
Feature: Chat about a note with AI
  Learner wants to chat with the AI about a certain note,
  so that they can understand the note better.


  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "There are 42 prefectures in Japan"
    And I start to chat about the note "There are 42 prefectures in Japan"


  Scenario: The users can conmunicate with AI
    Given OpenAI assistant will reply "No. It is not." for user message "Is Naba one of them?"
    When I send the message "Is Naba one of them?" to AI
    Then I should receive the following chat messages:
      | role      | message              |
      | user      | Is Naba one of them? |
      | assistant | No. It is not.       |


  @ignore
  Scenario: The users can continue to conmunication with AI
    Given OpenAI assistant will reply "I'm ChatGPT." for user message "What's your name?"
    And OpenAI assistant will reply "365" for user message "How many days are there in the year 2023?"
    When I send the message "What's your name?" to AI
    Then I should receive the following chat messages:
      | role      | message           |
      | user      | What's your name? |
      | assistant | I'm ChatGPT.     |
    When I send the message "How many days are there in the year 2023?" to AI
    Then I should receive the following chat messages:
      | role      | message                                   |
      | user      | What's your name?                         |
      | assistant | I'm ChatGPT.                              |
      | user      | How many days are there in the year 2023? |
      | assistant | 365                                       |
