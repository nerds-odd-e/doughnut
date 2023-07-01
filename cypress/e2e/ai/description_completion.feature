@usingMockedOpenAiService
Feature: Note description completion

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | description | testingParent |
      | Taiwan  |             |               |
      | Taipei  |             | Taiwan        |
      | Weather | It rains a  | Taipei        |

  Scenario: Generate Description
    Given OpenAI by default returns text completion "Pardon?"
    When I ask to complete the description for note "Taiwan"
    Then I should see the note description on current page becomes "Pardon?"

  Scenario: Open AI service availability
    Given open AI service always think the system token is invalid
    When I ask to complete the description for note "Taipei"
    Then I should see that the open AI service is not available in controller bar

  Scenario: Complete parts are displayed as soon as they are available
    Given OpenAI completes with "in the pacific ocean." for incomplete assistant message "A vigorous city,"
    And otherwise OpenAI returns an incomplete text completion "A vigorous city,"
    When I ask to complete the description for note "Taipei"
    Then I should see the note description on current page becomes "A vigorous city, in the pacific ocean."

  Scenario: AI will complete the description of a note
    But OpenAI completes with "It rains a lot." for incomplete assistant message "It rains a"
    When I ask to complete the description for note "Weather"
    Then I should see the note description on current page becomes "It rains a lot."

  Scenario: AI will complete the description of a note taking the context into consideration
    Given OpenAI completes with "It rains a lot." for context containing "Taiwan › Taipei"
    When I ask to complete the description for note "Weather"
    Then I should see the note description on current page becomes "It rains a lot."
