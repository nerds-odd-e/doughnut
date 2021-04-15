Feature: Provisioning Blog from Note

  Background:
    Given I've logged in as an existing user


  Scenario: provisioning Blog from Note
    When APIアドレスにNoteIdが渡ってアクセスされたとき
    Then Noteの内容が取得できる

#    Given "odd-e blog" という Blog Notebookがある
#    And  "how to do Scrum" という Blogがpostされている
#    When サードパーティアプリから"odd-e blog"を使う
#    Then サードパーティアプリから"how to do Scrum" というブログが見られる
