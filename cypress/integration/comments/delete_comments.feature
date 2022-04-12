Feature:
  User can delete comment which leave by himself

  Scenario: I delete comment
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    |
      | Sedition |
    And there are some comments for the note 'A'
      | content |
      | hello   |
      | world   |
    When I click "Delete" button on comment "Hello"
    Then Note 'A' only have one comment 'world'

  @ignore
  Scenario: I can see delete button
    Given I've logged in as an existing user
    And I create a note 'A'
    And there are some comments for the note 'A'
      | content |
      | hello   |
      | world   |
    When Someone leave a comment 'hello world!' on not 'A'
    Then I will see delete button on these comments
      | content      | delete |
      | hello        | true   |
      | world        | true   |
      | hello world! | false  |
