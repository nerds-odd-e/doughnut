Feature: Ask Statement


  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | title   | description | testingParent |
      | Taiwan  |             |               |
      | Taipei  | A           | Taiwan        |
      | Weather | It rains a  | Taipei        |
    And goto test me view of note "Taiwan"

  @ignore
  Scenario: The users can conmunicate with AI
    When I input the ask statement "What's your name?"
    And I clicked the ask button
    Then I can confirm the answer "I'm ChatGPT"


  @ignore
  Scenario: The users can continue to conmunication with AI
    When I input the ask statement "What's your name?"
    And I clicked the ask button
    Then I can confirm the answer include "I'm ChatGPT"
    When I input the ask statement "How many days are there in the year 2023?"
    And I clicked the ask button
    Then I can confirm the answer include "365"