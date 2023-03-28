Feature: Complete description
  As a learner, I want to complete description for
  my notes.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description         |
      | 台北的天氣  | 台北的冬天 |
      | 台北的天氣  |  |
      | 台北的天氣  | 不是空白 |
    And OpenAI always return text completion "Sharing the same planet as humans"

  @usingMockedOpenAiService
    @ignore
  Scenario: Perform action with completed note unfinished description
    When I ask for a description completion for "台北的冬天"
    Then I should be prompted with a suggested description "Sharing the same planet as humans"
    And I expect that the description will be "<value>" when I "<action>" the suggested description

  @ignore
  Scenario: See the robot icon with empty description
  @ignore
  Scenario: See the lightbulb icon with not empty description

