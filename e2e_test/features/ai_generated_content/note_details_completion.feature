@usingMockedOpenAiService
Feature: Note details completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | details    | parentTopic |
      | Taiwan           |            |             |
      | Taipei           | It is a    | Taiwan      |
      | Weather          | It rains a | Taipei      |

  Scenario: OpenAI Service Unavailability
    Given the OpenAI service is unavailable due to invalid system token
    When I request to complete the details for the note "Taipei"
    Then I should see a notification of OpenAI service unavailability in the controller bar

  Scenario: Completing Note Details Using OpenAI
    Given the OpenAI assistant will create a thread and request for the following actions:
      | response              | arguments         |
      | complete note details | " vigorous city." |
    When I request to complete the details for the note "Taipei"
    Then the note details on the current page should be "It is a vigorous city."
