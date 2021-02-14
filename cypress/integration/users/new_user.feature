Feature: new user

  Scenario: New user login using Github account
    When I login as a user
    Then I should be asked to create my profile
    When I save my profile without changing anything
    Then Account for "learner@doughnut.org" should have
      | user name |
      | Learner A |