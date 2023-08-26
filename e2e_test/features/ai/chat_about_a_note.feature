@usingMockedOpenAiService
Feature: Chat about a note with AI
  Learner wants to chat with the AI about a certain note,
  so that they can understand the note better.


  Background:
    Given I've logged in as an existing user
    And I have a note with the title "There are 42 prefectures in Japan"
    And OpenAI by default returns this question from now:
      | question                                 | correct_choice | incorrect_choice_1 |
      | How many prefectures are there in Japan? | 42             | 41                 |
    And I ask to generate a question for note "There are 42 prefectures in Japan"


  Scenario: The users can conmunicate with AI
    Given OpenAI by default returns text completion "I'm ChatGPT"
    When I chat to OpenAI "What's your name?"
    Then I can confirm the answer "I'm ChatGPT"


  Scenario: The users can continue to conmunication with AI
    Given OpenAI by default returns text completion "I'm ChatGPT"
    When I chat to OpenAI "What's your name?"
    Then I can confirm the answer "I'm ChatGPT"
    Given OpenAI by default returns text completion "365"
    When I chat to OpenAI "How many days are there in the year 2023?"
    Then I can confirm the answer "365"
