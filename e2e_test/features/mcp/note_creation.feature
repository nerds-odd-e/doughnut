Feature: Note creation

  Background:
    Given I connect to an MCP client that connects to Doughnut MCP service
    and user exists

  @ignore
  Scenario: Create new note under the default notebook
    Given I am logged in as an existing user
      And I have a valid authentication token on the MCP client
      And there is a default notebook "Notebook One"
    When I ask the MCP client to create a "Note One" note
    Then I have "Note One" note under "Notebook One" notebook
