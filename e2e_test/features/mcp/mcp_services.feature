@TerminateMCPServerWhenTeardown
Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Doughnut, so that AI can automatically update my notes and fetch information from my
  notes.

  Background:
    Given I am logged in as "old_learner"
    And I have a valid MCP token with label "For MCP services"
    And I connect to an MCP client that connects to Doughnut MCP service

  Scenario: Adding note to a known parent note
    Given I have a notebook with head note "Books I read" and notes:
      | Title             | Parent Title |
      | Lord of the Rings | Books I read |
      | Harry Potter      | Books I read |
    When AI agent adds note via MCP tool to add note "Art of War" under "Books I read"
    Then I should see "Books I read" with these children
      | note-title        |
      | Lord of the Rings |
      | Harry Potter      |
      | Art of War        |

  Scenario Outline: AI developer learns from Doughnut via MCP (happy case)
    Given I have a notebook with the head note "Lord of the Rings" and details "Test"
    And I have a notebook with the head note "Harry Potter" and details "Harry Potter is handsome"
    When AI agent searches for relevant notes using MCP tool with the term "<search_term>"
    Then the response should contain "<note_title>"

    Examples:
      | search_term | note_title              |
      | Lord        | Lord of the Rings       |
      | Harry       | Harry Potter            |
      | Fiona       | No relevant note found. |

  Scenario: AI agent gets relevant note and then fetches its graph using the token limit
    Given I have a notebook with head note "Programming Concepts" and notes:
      | Title                | Parent Title           |
      | Object Oriented      | Programming Concepts   |
      | Functional           | Programming Concepts   |
      | Classes              | Object Oriented        |
      | Inheritance          | Object Oriented        |
    When AI agent searches for relevant notes using MCP tool with the term "Object Oriented"
    Then the response should contain "Object Oriented"
    When AI agent extracts note ID from the search result and calls get graph MCP tool
    Then the graph response should contain the focus note "Object Oriented"
    And the graph response should contain related notes

  @skip
  Scenario: AI return a warning message when no MCP notebook is set
    Given I have a notebook with the head note "Lord of the Rings" and details "Test"
    When AI agent searchs for relevant notes using MCP tool with the term "Lord"
    Then the response should contain "No MCP notebook is set for this user."

  @skip
  Scenario: AI agent respects different token limits for graph retrieval

  Scenario Outline: AI agent respects different token limits for graph retrieval
    Given I have a notebook with head note "Programming Concepts" and notes:
      | Title           | Parent Title         |
      | Object Oriented | Programming Concepts |
      | Functional      | Programming Concepts |
      | Classes         | Object Oriented      |
      | Inheritance     | Object Oriented      |

    When AI agent searches for relevant notes using MCP tool with the term "Functional"
    Then the response should contain "Functional"
    When AI agent extracts note ID and calls get graph MCP tool with token limit "<token_limit>"
    Then the graph response should show "<expected_behavior>"

    Examples:
      | token_limit | expected_behavior                    |
      | 0           | tokenLimit must be a positive number |
      | 10          | Programming Concepts                 |
      | 1000        | Functional                           |
