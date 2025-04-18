Feature: get username
  Return username

  @ignore
  Scenario: get username
    Given MCP server is running
    And User have valid MCP token
    When Call get username tool by MCP Client
    Then Return username