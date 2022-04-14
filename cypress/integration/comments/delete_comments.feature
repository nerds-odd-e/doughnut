Feature:
  User can delete comment which leave by himself

  @ignore
  @featureToggle
  Scenario: I delete comment
    Given I've logged in as an existing user
    And there is a note and some comments of current user
      | content     |
      | hello world |
      | world       |
    When I delete comment "hello world" under Note 'A'
    Then Note 'A' only have one comment 'world'
