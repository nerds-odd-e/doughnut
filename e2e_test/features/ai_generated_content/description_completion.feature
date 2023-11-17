@usingMockedOpenAiService
Feature: Note details completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details     | testingParent |
      | Taiwan  |             |               |
      | Taipei  | A           | Taiwan        |
      | Weather | It rains a  | Taipei        |

  Scenario: Open AI service availability
    Given open AI service always think the system token is invalid
    When I ask to complete the details for note "Taipei"
    Then I should see that the open AI service is not available in controller bar

  Scenario: AI will complete the details of a note
    But OpenAI completes with " vigerous city." for incomplete note details "A"
    When I ask to complete the details for note "Taipei"
    Then I should see the note details on current page becomes "A vigerous city."

  Scenario: AI will complete the details of a note taking the context into consideration
    Given OpenAI completes with "It rains a lot." for context containing "Taiwan › Taipei"
    When I ask to complete the details for note "Weather"
    Then I should see the note details on current page becomes "It rains a lot."
