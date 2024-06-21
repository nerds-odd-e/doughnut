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

  Scenario Outline: Responding to AI's Clarification Question
    Given the OpenAI assistant will create a thread and request for the following actions:
      | response                   | arguments         |
      | ask clarification question | What do you mean? |
      | complete note details      | " lot."           |
    When I request to complete the details for the note "Weather"
    And I <respond> to the clarifying question "What do you mean?"
    Then the note details on the current page should be "<note details>"

    Examples:
      | respond                | note details    |
      | answer "Does it rain?" | It rains a lot. |
      | respond with "cancel"  | It rains a      |

  Scenario: Managing Extended Clarification Dialogue
    Given the OpenAI assistant will create a thread and request for the following actions:
      | response                   | arguments                                           |
      | ask clarification question | What do you mean? |
      | ask clarification question | Do you mean in general or near future?                |
    When I request to complete the details for the note "Weather"
    And I answer "Does it rain?" to the clarifying question "What do you mean?"
    Then I should see a follow-up question "Do you mean in general or near future?"
    And the initial clarifying question with the response "Does it rain?" should be visible
