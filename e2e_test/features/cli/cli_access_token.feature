@withCliConfig
@interactiveCLI
Feature: CLI access token management

  Background:
    Given I am logged in as "old_learner"

  Scenario: Add access token and list it
    And I have a valid Doughnut Access Token with label "E2E CLI Token"
    When I add the saved access token in the interactive CLI using add-access-token
    When I enter the slash command "/list-access-token" in the interactive CLI
    Then I should see "E2E CLI Token" in the Current guidance

  Scenario: Add invalid access token
    When I enter the slash command "/add-access-token invalid-token-xxx" in the interactive CLI
    Then I should see "Access token is invalid or expired" in past CLI assistant messages

  @ignore
  Scenario Outline: Remove access token
    And I have a valid Doughnut Access Token with label "<label>"
    When I add the saved access token in the interactive CLI using add-access-token
    When I enter the slash command "<action> <label>" in the interactive CLI
    Then I should see the <removal_type> remove success message for "<label>"
    When I enter the slash command "/list-access-token" in the interactive CLI
    Then I should see "No access tokens stored." in past CLI assistant messages

    Examples:
      | label           | action                          | removal_type |
      | Remove Me Token | /remove-access-token            | local        |
      | Revoke Me Token | /remove-access-token-completely | complete     |

  @ignore
  Scenario: Another key cancels remove-access-token selection
    And I have a valid Doughnut Access Token with label "E2E CLI Token"
    When I add the saved access token in the interactive CLI using add-access-token
    When I enter the slash command "/remove-access-token" in the interactive CLI
    Then I should see "E2E CLI Token" in the Current guidance
    When I cancel the token list with q in the interactive CLI
    Then I should see "/ commands" in the Current guidance
    When I enter the slash command "/list-access-token" in the interactive CLI
    Then I should see "E2E CLI Token" in the Current guidance

  @ignore
  Scenario: Create access token via CLI
    And I have a valid Doughnut Access Token with label "Default Token"
    When I add the saved access token in the interactive CLI using add-access-token
    When I enter the slash command "/create-access-token New CLI Token" in the interactive CLI
    Then I should see "Token created" in past CLI assistant messages
    When I enter the slash command "/list-access-token" in the interactive CLI
    Then I should see "New CLI Token" in the Current guidance
