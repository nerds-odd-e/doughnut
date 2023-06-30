Feature: Generate image
  To play with the Open AI DALL-E API, I want to generate an image from text.

  Background:
    Given I've logged in as an existing user
    And I have a note with the title "Animals"

  @usingMockedOpenAiService
  Scenario: get an image for a note based on its title
    Given OpenAI always return image of a moon
    When I generate an image for "Animals"
    Then I should find an art created by the ai

  @usingMockedOpenAiService
  Scenario: fail to generate image because the serivce is not available
    Given An OpenAI response is unavailable
    When I generate an image for "Animals"
    Then I should be prompted with an error message saying "There is a problem"

