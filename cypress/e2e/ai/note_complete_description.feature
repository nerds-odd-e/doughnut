Feature: 筆記描述補全功能
  身為lerner，我希望可以幫我補全描述

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description |
      | 台北的天氣    | 台北的冬天       |
      | 描述為空的筆記  |             |
      | 描述不為空的筆記 | 不是空白        |

  @usingMockedOpenAiService
  @ignore
  Scenario: 有描述時可以使用補全功能
    Given 我打開標題為"台北的天氣"這個筆記
    And AI會基於"台北的冬天"得到補全描述"台北冬天每天都在下雨,晴天的機率可能比刮刮樂還低"
    When 要求補全描述
    Then 描述會變成"台北冬天每天都在下雨,晴天的機率可能比刮刮樂還低"
