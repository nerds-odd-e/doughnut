@usingMockedOpenAiService
Feature: Note details completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details       | testingParent |
      | Taiwan  |             |               |
      | Taipei  | A           | Taiwan        |
      | Weather | It rains a  | Taipei        |

  Scenario: Generate Details
    Given OpenAI by default returns text completion "Pardon?"
    When I ask to complete the details for note "Taiwan"
    Then I should see the note details on current page becomes "Pardon?"

  Scenario: Open AI service availability
    Given open AI service always think the system token is invalid
    When I ask to complete the details for note "Taipei"
    Then I should see that the open AI service is not available in controller bar

  Scenario: Complete parts are displayed as soon as they are available
    Given OpenAI returns an incomplete text completion "vigorous city," for assistant message "A"
    Given OpenAI completes with "in the pacific ocean." for assistant message "A vigorous city,"
    When I ask to complete the details for note "Taipei"
    Then I should see the note details on current page becomes "A vigorous city, in the pacific ocean."

  Scenario: AI will complete the details of a note
    But OpenAI completes with "It rains a lot." for assistant message "It rains a"
    When I ask to complete the details for note "Weather"
    Then I should see the note details on current page becomes "It rains a lot."

  Scenario: AI will complete the details of a note taking the context into consideration
    Given OpenAI completes with "It rains a lot." for context containing "Taiwan › Taipei"
    When I ask to complete the details for note "Weather"
    Then I should see the note details on current page becomes "It rains a lot."
