@usingMockedOpenAiService
Feature: Notebook assistant
  As a notebook keeper, I want to create an AI assistant that is aware of
  my notebook content and can assistant myself and people who subscribed to
  my notebook on the related topics.


  Background:
    Given I am logged in as an admin
    And there are some notes for the current user:
      | topicConstructor | details    | parentTopic |
      | Vertical farming |            |             |
      | Acqua            |            | Vertical farming  |


  @ignore
  Scenario: The users will use the notebook assistant if exist
    Given I create an assistant for my notebook "Vertical farming" assuming the assistant id "assistant-id-1"
    Then it should use assistant id "assistant-id-1"
    When I start to chat about the note "Acqua"
