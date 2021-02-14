Feature: new user

  Scenario: New user creating account
    When I identify myself as a new user
    Then I should be asked to create my profile
    When I save my profile without changing anything
    Then Account for "learner@doughnut.org" should have
      | user name |
      | Learner A |