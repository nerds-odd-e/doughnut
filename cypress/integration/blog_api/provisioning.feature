Feature: Provisioning Blog from Note

  Background:
    Given I've logged in as an existing user

  @ignore
  Scenario: provisioning Blog from Note
    When APIアドレスにNoteIdが渡ってアクセスされたとき
    Then Noteの内容が取得できる
