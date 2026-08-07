Feature: New user registration

  Scenario: New user creates a profile
    Given I am on the sign-in page
    When I identify myself as a new user
    Then I should be asked to create my profile
    When I save my profile with:
      | Name      |
      | Learner A |
    Then I should see the home welcome heading for user "Learner A"
