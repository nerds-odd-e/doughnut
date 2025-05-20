Feature: Generate Token

  Scenario: Generate token
    Given I am logged in as an existing user
    And I am on generate token page
    When I click generate token button
    Then I should see token string in the page

  Scenario: Generate token when I haven't login
    Given I haven't login
    When I am on generate token page
    Then I should see message that says "You need to be logged in to generate token."

  Scenario: Generate token when session is expired
    Given I am logged in as an existing user
    And I am on generate token page
    And my session is logged out
    When I click generate token button
    Then I should see message that says "You need to be logged in to generate token."
    