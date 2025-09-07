@TerminateMCPServerWhenTeardown
Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Doughnut, so that AI can automatically update my notes and fetch information from my
  notes.

  Background:
    Given I am logged in as "old_learner"
    And I have a valid MCP token
    And I connect to an MCP client that connects to Doughnut MCP service

  Scenario: Get notebook list
    Given I have a notebook with the head note "Lord of the Rings"
    And I have a notebook with the head note "Harry Potter"
    When AI agent calls the "get_notebook_list" MCP tool
    Then the response should contain "Lord of the Rings"
    Then the response should contain "Harry Potter"

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
    When AI agent searchs for relevant notes using MCP tool with the term "<search_term>"
    Then the response should contain "<note_title>"

    Examples:
      | search_term | note_title              |
      | Lord        | Lord of the Rings       |
      | Harry       | Harry Potter            |
      | Fiona       | No relevant note found. |
