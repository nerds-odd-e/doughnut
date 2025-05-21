Feature: MCP (Model Context Protocol) get notebook list

  @ignore
  Scenario: Get notebook list
    Given I am logged in as an existing user
    And I have a valid MCP token
    And I connect to an MCP client that connects to Doughnut MCP service with my MCP token
    And I have a notebook with the head note "Lord of the Rings"
    And I have a notebook with the head note "Harry Potter"
    When I request MCP server to get the notebook list
    Then I should receive a list of notebooks in the MCP response: "Lord of Rings, Harry Potter"