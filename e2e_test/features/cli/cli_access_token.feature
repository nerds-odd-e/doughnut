@withCliConfig
Feature: CLI access token management

  Background:
    Given I am logged in as "old_learner"

  Scenario: Add access token and list it
    And I have a valid Doughnut Access Token with label "E2E CLI Token"
    When I run doughnut -c "/add-access-token" with the saved token
    Then I should see "Token added" in the non-interactive output
    When I run the doughnut command with -c "/list-access-token"
    Then I should see "E2E CLI Token" in the non-interactive output

  Scenario: Add invalid access token
    When I run doughnut -c "/add-access-token" with token "invalid-token-xxx"
    Then I should see "Token is invalid or expired." in the non-interactive output

  Scenario Outline: Remove access token
    And I have a valid Doughnut Access Token with label "<label>"
    When I run doughnut -c "/add-access-token" with the saved token
    Then I should see "Token added" in the non-interactive output
    When I run doughnut -c "<action>" with label "<label>"
    Then I should see the <removal_type> remove success message for "<label>"
    When I run the doughnut command with -c "/list-access-token"
    Then I should see "No access tokens stored." in the non-interactive output

    Examples:
      | label           | action                          | removal_type |
      | Remove Me Token | /remove-access-token            | local        |
      | Revoke Me Token | /remove-access-token-completely | complete     |

  
  @interactiveCLI
  Scenario: ESC cancels remove-access-token selection
    And I have a valid Doughnut Access Token with label "E2E CLI Token"
    When I run doughnut -c "/add-access-token" with the saved token
    Then I should see "Token added" in the non-interactive output
    When I input "/remove-access-token" in the interactive CLI
    Then I should see "E2E CLI Token" in the Current guidance
    When I press ESC in the interactive CLI
    Then I should see "/ commands" in the Current guidance
    When I input "/list-access-token" in the interactive CLI
    Then I should see "E2E CLI Token" in the Current guidance

  Scenario: Create access token via CLI
    And I have a valid Doughnut Access Token with label "Default Token"
    When I run doughnut -c "/add-access-token" with the saved token
    Then I should see "Token added" in the non-interactive output
    When I run doughnut -c "/create-access-token" with label "New CLI Token"
    Then I should see "Token created" in the non-interactive output
    When I run the doughnut command with -c "/list-access-token"
    Then I should see "New CLI Token" in the non-interactive output
