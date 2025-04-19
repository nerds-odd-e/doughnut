Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Doughnut, so that AI can automatically update my notes and fetch informat from my
  notes.

  Background:
    Given I connect to an MCP client that connects to Doughnut MCP service

  Scenario: get instruction
    When Call instruction API by MCP Client
    Then Return Doughnut instruction

  Scenario: get username
    Given I am logged in as an existing user
    And I generate MCP Token
    When Call get username tool by MCP Client
    Then Return username
