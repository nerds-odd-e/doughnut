Feature: Note description completion

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description | parentTitle |
      | Taiwan | 台北 best     | N.A.        |
      | 台北的天氣  | 台北的冬天       | Taiwan      |

  @Ignore
  @usingMockedOpenAiService
  Scenario: AI will complete the description of a note taking into consideration all titles within same path and existing description
    Given OpenAI returns text completion "台北冬天每天都在下雨,晴天的機率可能比刮刮樂還低" for prompt "Taiwan 台北的天氣 台北的冬天"
    When I ask to complete the description for note "台北的天氣"
    Then I should see the note description on current page becomes "台北冬天每天都在下雨,晴天的機率可能比刮刮樂還低"
