Feature: User MCP token
  As a user, I want to use MCP token to access Doughnut MCP service.

  Background:
    Given I am logged in as "old_learner"

  Scenario: Generate MCP token
    When I generate a new MCP token with label "Initial Token"
    Then the new MCP Token should be a valid UUID

  Scenario Outline: Creating a new MCP token with a label
    Given I have no MCP token with label "<token_label>"
    When I create an MCP token with label "<token_label>"
    Then I can see the token with label "<token_label>" in the list of tokens

    Examples:
      | token_label |
      | Lord        |
      | Harry       |
      | Fiona       |

  Scenario: Delete an MCP token
    Given I have a valid MCP token with label "To be deleted"
    When I delete the MCP token with label "To be deleted"
    # Then I cannot use the token to access MCP services
  
  Scenario: User MCP token expiration
    Given I have a valid MCP token with label "my token"
    #When it is 90 days later
    Then the MCP token "my token" should be marked as expired
