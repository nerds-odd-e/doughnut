Feature: CLI access token management

  Scenario: Add access token and list it
    Given I am logged in as "old_learner"
    And I have a valid Doughnut Access Token with label "E2E CLI Token"
    And I have a CLI config directory
    When I run the doughnut CLI add-access-token with the saved token
    Then I should see "Token added"
    When I run the doughnut command with -c "/list-access-token" using CLI config
    Then I should see "E2E CLI Token"

  Scenario: Add invalid access token
    Given I have a CLI config directory
    When I run the doughnut CLI add-access-token with token "invalid-token-xxx"
    Then I should see "Token is invalid or expired."

  Scenario: Remove access token locally
    Given I am logged in as "old_learner"
    And I have a valid Doughnut Access Token with label "Remove Me Token"
    And I have a CLI config directory
    When I run the doughnut CLI add-access-token with the saved token
    Then I should see "Token added"
    When I run the doughnut command with -c "/remove-access-token Remove Me Token" using CLI config
    Then I should see "Token \"Remove Me Token\" removed."
    When I run the doughnut command with -c "/list-access-token" using CLI config
    Then I should see "No access tokens stored."

  Scenario: Remove access token completely (local and server)
    Given I am logged in as "old_learner"
    And I have a valid Doughnut Access Token with label "Revoke Me Token"
    And I have a CLI config directory
    When I run the doughnut CLI add-access-token with the saved token
    Then I should see "Token added"
    When I run the doughnut CLI remove-access-token-completely with label "Revoke Me Token"
    Then I should see "removed locally and from server"
    When I run the doughnut command with -c "/list-access-token" using CLI config
    Then I should see "No access tokens stored."
