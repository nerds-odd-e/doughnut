@BundleFirstAndTerminateMCPServerWhenTeardown
Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Donut, so that AI can automatically update my notes and fetch information from my
  notes.

  Background:
    Given I am logged in as "old_learner"
    And I have a valid Donut Access Token with label "For MCP services"
    And I connect to an MCP client that connects to Donut MCP service
    And I have a notebook "CS concepts" with notes:
      | Title           | Folder               |
      | Object Oriented | Programming Concepts |
      | Functional      | Programming Concepts |

  Scenario Outline: AI agent finds relevant notes via MCP
    When AI agent searches for relevant notes using MCP tool with the term "<search_term>"
    Then the response should contain "<note_title>"

    Examples:
      | search_term     | note_title              |
      | Object Oriented | Object Oriented         |
      | Functional      | Functional              |
      | Fiona           | No relevant note found. |

  Scenario Outline: AI agent respects token limits when retrieving a note graph
    When AI agent searches for relevant notes using MCP tool with the term "Functional"
    Then the response should contain "Functional"
    When AI agent extracts note ID and calls get graph MCP tool with token limit "<token_limit>"
    Then the graph response should contain the focus note "Functional"
    And the graph response should <sibling_inclusion> "Object Oriented"

    Examples:
      | token_limit | sibling_inclusion |
      | 10          | not contain       |
      | 1000        | contain           |
