@TerminateMCPServerWhenTeardown
Feature: MCP Notebook Selection
  As a note taker, I want my AI clients to be able to use the notebook I selected as MCP notebook,
  and use it to fetch and store information from my notes.

  Background:
    Given I am logged in as "old_learner"
    And I have a valid MCP token with label "For MCP services"
    And I connect to an MCP client that connects to Doughnut MCP service

  @skip
  Scenario: AI return a warning message when no MCP notebook is set
    Given I have a notebook with the head note "Lord of the Rings" and details "Test"
    When AI agent searches for relevant notes using MCP tool with the term "Lord"
    Then the response should contain "Inform the user that -no MCP notebook is set.-"

  Scenario: User selects a notebook as MCP_Notebook
    Given I have a notebook with the head note "Cats" and details "nice cat"
    When I select the notebook "Cats" as MCP_Notebook
    Then I see the "MCP" banner for notebook "Cats"
