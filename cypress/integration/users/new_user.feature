Feature: new user

  Scenario: New user creating account
    Given I'm on the login page
    When I identify myself as a new user
    Then I should be asked to create my profile
    When I save my profile with:
      | Name      |
      | Learner A |
    Then I should see "Welcome Learner A" in the page
    And My name "Learner A" is in the user action menu

