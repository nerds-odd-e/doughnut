Feature:
  User can reply comment when comment exists

  @ignore
  @featureToggle
  Scenario: I reply a comment
    Given I've logged in as an existing user
    And there is a note and some comments of current user
      | comment |
      | hello   |
      | hello2  |
    And I visit note 'A'
    When I reply to comment 'hello' with 'world'
    Then I should see comments
      | comments |
      | hello    |
      | hello2   |
      | world    |

