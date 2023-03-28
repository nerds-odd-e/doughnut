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
    Given Title is "台北的天氣"
    And 我要求AI補全"台北的冬天"時，AI永遠return"台北冬天每天都在下雨,晴天的機率可能比刮刮樂還低"
    When 我在描述輸入"台北冬天"，並且要求補全描述
    Then 描述會變成"台北冬天每天都在下雨,晴天的機率可能比刮刮樂還低"

  @ignore
  Scenario: See the robot icon with empty description
  @ignore
  Scenario: See the lightbulb icon with not empty description

