Feature: new user

  Scenario: New user login using Github account
    Given There is a Github user with
      | user name | email                |
      | Learner A | learner@doughnut.org |
    When I login using Github account "learner@doughnut.org" successfully
    Then I should be asked to create my profile
    When I save my profile without changing anything
    Then Account for "learner@doughnut.org" should have
      | user name |
      | Learner A |