@usingMockedOpenAiService
Feature: Chat about a note with AI
  Learner wants to chat with the AI about a certain note,
  so that they can understand the note better.


  Background:
    Given I've logged in as an existing user
    And I have a note with the title "There are 42 prefectures in Japan"
    And I start to chat about the note "There are 42 prefectures in Japan"


  Scenario: The users can conmunicate with AI
    Given OpenAI completes with "No. It is not." for messages containing:
      | role   | content                           |
      | system | There are 42 prefectures in Japan |
      | user   | Is Naba one of them?              |
    When I send the message "Is Naba one of them?" to AI
    Then I should receive the response "No. It is not."


  Scenario: The users can continue to conmunication with AI
    Given OpenAI by default returns text completion "I'm ChatGPT"
    When I send the message "What's your name?" to AI
    Then I should receive the response "I'm ChatGPT"
    Given OpenAI by default returns text completion "365"
    When I send the message "How many days are there in the year 2023?" to AI
    Then I should receive the response "365"
