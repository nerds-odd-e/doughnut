Feature: Tell me a engaging story
  As a learner, I want to the notes to be summarized like an engaging story

  Background:
    Given I've logged in as an existing user
    And I have a note with the title "Animals"

  @usingMockedOpenAiService
  Scenario: get an engaging story for a note
    Given OpenAI always return image of a moon
    When I ask for an engaging story for "Animals"
    Then I should find an art created by the ai

  @usingMockedOpenAiService
  Scenario: fail to get an engaging story for a note because the serivce is not available
    Given An OpenAI response is unavailable
    When I ask for an engaging story for "Animals"
    Then I should be prompted with an error message saying "There is a problem"

