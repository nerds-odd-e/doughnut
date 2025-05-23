Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Doughnut, so that AI can automatically update my notes and fetch information from my
  notes.

  Background:
    Given I am logged in as "old_learner"
    And I have a valid MCP token
    And I connect to an MCP client that connects to Doughnut MCP service

  Scenario Outline: MCP API calls
    When I call the "<api_name>" MCP tool
    Then the response should equal "<expected_response>"

    Examples:
      | api_name        | expected_response                               |
      | get_instruction  | Doughnut is a Personal Knowledge Management tool |

    @ignore
    Scenario: Get notebook list
    Given I have a notebook with the head note "Lord of the Rings"
    And I have a notebook with the head note "Harry Potter"
    When I call the "get_notebook_list" MCP tool
    Then I should receive a list of notebooks in the MCP response contain "Lord of the Rings, Harry Potter"

  @ignore
  Scenario Outline: Update note title/detail
    When I call the "<api_name>" MCP tool
    And I update a note title with this id "<note_it>" to "new_title"
    Then the response should contain "<expected_response>"

    Examples:
      | api_name | note_it | new_title | expected_response |
      | update_note_text_content | n12345 | Cat | Note updated successfully. |
      | update_note_text_content | n12346 | Cat | Failed to update note |

  Scenario Outline: Retrieve basic user information
    When call Mcp server get_user_info API
    Then the response should return user name contain "<userName>"

    Examples:
      | userName    |
      | Old Learner |


  Scenario Outline: Retrieve graph with note id
    When the client requests read note with graph from "<noteId>" via MCP service
    Then the response should return a json object contain "<expected_response>"
    #And the json is correctly formatted

    Examples:
      | noteId      | expected_response                               |
      | 100   | blank                               |
