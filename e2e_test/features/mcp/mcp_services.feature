@BundleFirstAndTerminateMCPServerWhenTeardown
Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Doughnut, so that AI can automatically update my notes and fetch information from my
  notes.

  Background:
    Given I am logged in as "old_learner"
    And I have a valid Doughnut Access Token with label "For MCP services"
    And I connect to an MCP client that connects to Doughnut MCP service
    And I have a notebook "CS concepts" with a note "Programming Concepts" and notes:
      | Title           | Parent Title         | Folder                              |
      | Object Oriented | Programming Concepts | Programming Concepts                |
      | Functional      | Programming Concepts | Programming Concepts                |
      | Classes         | Object Oriented      | Programming Concepts/Object Oriented |
      | Inheritance     | Object Oriented      | Programming Concepts/Object Oriented |

  Scenario: Adding note to a known parent note
    When AI agent adds note via MCP tool to add note "Procedural" under "Programming Concepts"
    Then I should see folder "CS concepts/Programming Concepts" containing these notes:
      | note-title      |
      | Object Oriented |
      | Functional      |
      | Procedural      |

  Scenario Outline: AI agent learns from Doughnut via MCP client
    When AI agent searches for relevant notes using MCP tool with the term "<search_term>"
    Then the response should contain "<note_title>"

    Examples:
      | search_term     | note_title              |
      | Object Oriented | Object Oriented         |
      | Functional      | Functional              |
      | Fiona           | No relevant note found. |

  Scenario Outline: AI agent respects different token limits for graph retrieval
    When AI agent searches for relevant notes using MCP tool with the term "Functional"
    Then the response should contain "Functional"
    When AI agent extracts note ID and calls get graph MCP tool with token limit "<token_limit>"
    Then the graph response should contain the focus note "Functional"
    And the graph response should <sibling_inclusion> "Object Oriented"

    Examples:
      | token_limit | sibling_inclusion |
      | 10          | not contain       |
      | 1000        | contain           |
