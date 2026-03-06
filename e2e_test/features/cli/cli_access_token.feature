Feature: CLI access token management

  Scenario: Add access token and list it
    Given I have a CLI config directory
    When I run the doughnut command with -c "/add-access-token test-token-123" using CLI config
    Then I should see "Token added"
    When I run the doughnut command with -c "/list-access-token" using CLI config
    Then I should see "test"
