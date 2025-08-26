
@ignore
@TerminateMCPServerWhenTeardown
Feature: AI developer learns from Doughnut via MCP
  As a new developer, I want to use AI to learn from doughnut knowledge base via MCP

  Background:
    Given I am logged in as "old_learner"
    And I have a valid MCP token
    And I connect to an MCP client that connects to Doughnut MCP service
    And I have a notebook with the head note "Lord of the Rings"
    And I have a notebook with the head note "Harry Potter"

  Scenario Outline: AI developer learns from Doughnut via MCP (happy case)
    When I search for notes with the term "<search_term>"
    Then the search results should include a note with the title "<note_title>"
    When I get the note ID from the search result for "<note_title>"
    And I call the "get_graph_with_note_id" MCP tool with that note ID
    Then the response should contain "<note_title>"
    And the response should contain "focusNote"

    Examples:
      | search_term | note_title           |
      | Lord        | Lord of the Rings    |
      | Harry       | Harry Potter         |

  Scenario Outline: AI developer learns from Doughnut via MCP (unhappy case)
    When I search for notes with the term "<search_term>"
    Then the search results should not include a note with the title "<note_title>"
    Then the search results should be blank

    Examples:
      | search_term | note_title |
      | Frodo       |           |
      | Hermione    |           |
