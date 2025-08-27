@TerminateMCPServerWhenTeardown
Feature: AI developer learns from Doughnut via MCP
  As a new developer, I want to use AI to learn from doughnut knowledge base via MCP

  Background:
    Given I am logged in as "old_learner"
    And I have a valid MCP token
    And I connect to an MCP client that connects to Doughnut MCP service
    And I have a notebook with the head note "Lord of the Rings" and details "Test"
    And I have a notebook with the head note "Harry Potter" and details "Harry Potter is handsome"

  Scenario Outline: AI developer learns from Doughnut via MCP (happy case)
    When I search for notes with the term "<search_term>" 
    Then the response should contain "<note_title>"

    Examples:
      | search_term | note_title           |
      | Lord        | Lord of the Rings    |
  #    | Harry       | Harry Potter         |

  Scenario Outline: AI developer learns from Doughnut via MCP (unhappy case)
    When I search for notes with the term "<search_term>"
    Then the response should contain "No relevant note found."

    Examples:
      | search_term |
      | Frodo       |
      | Hermione    |
