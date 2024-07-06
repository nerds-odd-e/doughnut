@usingMockedOpenAiService
Feature: Notebook assistant
  As a notebook keeper, I want to create an AI assistant that is aware of
  my notebook content and can assistant myself and people who subscribed to
  my notebook on the related topics.


  Background:
    Given I am logged in as an admin
    And there are some notes for the current user:
      | Topic            | Details | Parent Topic     |
      | Vertical farming |         |                  |
      | Acquaponics      |         | Vertical farming |


  Scenario: The users will use the notebook assistant if exist
    Given OpenAI creates an assistant of ID "assistant-id-1" for name "Assistant for notebook Vertical farming" with additional instruction "Please use simple English."
    And OpenAI accepts the vector file upload requests
    And I create an assistant for my notebook "Vertical farming" with additional instruction "Please use simple English."
    And OpenAI assistant "assistant-id-1" will reply below for user messages:
      | user message          | assistant reply                   | run id |
      | Tell me more about it | It is a kind of vertical farming. | run1   |
    When I start to chat about the note "Acquaponics"
    And I send the message "Tell me more about it" to AI
    Then I should receive the following chat messages:
      | role      | message                           |
      | assistant | It is a kind of vertical farming. |
