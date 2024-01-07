@usingMockedOpenAiService
Feature: Note details completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details     | testingParent |
      | Taiwan  |             |               |
      | Taipei  | It is a     | Taiwan        |
      | Weather | It rains a  | Taipei        |

  Scenario: OpenAI Service Unavailability
    Given the OpenAI service is unavailable due to invalid system token
    When I request to complete the details for the note "Taipei"
    Then I should see a notification of OpenAI service unavailability in the controller bar

  Scenario: Completing Note Details Using OpenAI
    Given OpenAI service can create thread and run with id "thread-111" when requested
    Given the OpenAI assistant in thread "thread-111" is set to:
      | response | arguments           |
      | complete | " vigorous city."   |
    When I request to complete the details for the note "Taipei"
    Then the note details on the current page should be "It is a vigorous city."
