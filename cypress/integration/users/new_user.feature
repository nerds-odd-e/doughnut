Feature: new user

  Scenario: New user creating account
    When I identify myself as a new user
    Then I should be asked to create my profile
    When I save my profile with:
      | name      |
      | Learner A |
    Then Account for "learner@doughnut.org" should have
