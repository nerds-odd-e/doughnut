Feature:
  User can delete comment which leave by himself

  @ignore
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
