@usingMockedOpenAiService
Feature: Notebook assistant
  As a notebook keeper, I want to create an AI assistant that is aware of
  my notebook content and can assistant myself and people who subscribed to
  my notebook on the related topics.


  Background:
    Given I am logged in as an admin
    And I have a notebook with head note "Vertical farming" and notes:
      | Title       | Parent Title     |
      | Acquaponics | Vertical farming |


  Scenario: The users will use the notebook assistant if exist
    Given OpenAI creates an assistant of ID "assistant-id-1" for name "Assistant for notebook Vertical farming" with model "gpt-4o-mini"
    And OpenAI accepts the vector file upload requests
    And I set my notebook "Vertical farming" to use additional AI instruction "Please use simple English."
    And I create a customized assistant for my notebook "Vertical farming"
    And OpenAI will reply below for user messages with notebook-specific instructions:
      | user message          | assistant reply                   |
      | Tell me more about it | It is a kind of vertical farming. |
    When I start to chat about the note "Acquaponics"
    And I send the message "Tell me more about it" to AI
    Then I should receive the following chat messages:
      | role      | message                           |
      | assistant | It is a kind of vertical farming. |
