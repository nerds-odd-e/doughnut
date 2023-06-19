Feature: Note description completion

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | description | testingParent |
      | Taiwan  |             |               |
      | Taipei  |             | Taiwan        |
      | Weather | It rains a  | Taipei        |
    Given OpenAI always returns text completion "Pardon?"

  @usingMockedOpenAiService
  Scenario: AI will complete the description of a note
    But OpenAI completes with "It rains a lot." for incomplete assistant message "It rains a"
    When I ask to complete the description for note "Weather"
    Then I should see the note description on current page becomes "It rains a lot."

  @usingMockedOpenAiService
  Scenario: AI will complete the description of a note taking the context into consideration
    Given OpenAI completes with "It rains a lot." for context containing "Taiwan › Taipei"
    When I ask to complete the description for note "Weather"
    Then I should see the note description on current page becomes "It rains a lot."
