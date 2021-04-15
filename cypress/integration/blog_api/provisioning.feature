Feature: Provisioning Blog from Note

  Background:
    Given I've logged in as an existing user


  Scenario: provisioning Blog from Note
    Given odd-e blog という Blog Notebookがある
      | Title    | Description     |
      | odd-e blog | test |
    And  how to do Scrum という Blogがpostされている
      | Title    | Description     |
      | how to do Scrum | Scrum |
    When サードパーティアプリから odd-e blog api を使う
    Then サードパーティアプリから how to do Scrum というブログが見られる
    When odd-e blog api をリクエストすると
    Then ノートオブジェクトが取得できること
