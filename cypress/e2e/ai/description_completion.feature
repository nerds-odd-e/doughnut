Feature: Note description completion

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | description | testingParent |
      | Taiwan  |             |               |
      | Taipei  |             | Taiwan        |
      | Weather | It rains a  | Taipei        |


  @usingMockedOpenAiService
  Scenario: AI will complete the description of a note
    Given OpenAI returns text completion "It rains a lot." for prompt containing "It rains a"
    When I ask to complete the description for note "Weather"
    Then I should see the note description on current page becomes "It rains a lot."

  @ignore
  @usingMockedOpenAiService
  Scenario: AI will complete the description of a note taking the context into consideration
    Given OpenAI returns text completion "It rains a lot." for prompt containing "It rains a" and context containing "Taiwan/Taipei/Weather"
    When I ask to complete the description for note "Weather"
    Then I should see the note description on current page becomes "It rains a lot."
