@TerminateMCPServerWhenTeardown
Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Doughnut, so that AI can automatically update my notes and fetch information from my
  notes.

  Background:
    Given I am logged in as "old_learner"
    And I have a valid MCP token
    And I connect to an MCP client that connects to Doughnut MCP service

  Scenario Outline: MCP Tools
    When I call the "<api_name>" MCP tool
    Then the response should contain "<expected_response>"

    Examples:
      | api_name        | expected_response                                |
      | get_instruction | Doughnut is a Personal Knowledge Management tool |
      | get_user_info   | Old Learner                                      |

 @ignore
  Scenario: Get notebook list
    Given I have a notebook with the head note "Lord of the Rings"
    And I have a notebook with the head note "Harry Potter"
    When I call the "get_notebook_list" MCP tool
    Then the response should contain "Lord of the Rings"
    Then the response should contain "Harry Potter"

  @ignore
  Scenario: Add note to notebook
    Given I have a notebook with head note "Books I read" and notes:
      | Title             | Parent Title | 
      | Lord of the Rings | Books I read | 
      | Harry Potter      | Books I read | 
    #And the phrase "Lord of the Rings" and "suitable parent for `Frodo`" have similarity distance of 0.3 [mock]
    And The only suitable parent for phrase "Art of War" is "Books I read"
    When AI agent add note via MCP tool to add note "Art of War" under "Books I read"
    Then I should see the note tree in the sidebar
      | note-title        |
      | Lord of the Rings |
      | Harry Potter      |
      | Art of War        |
    And I should see "Books I read" with these children
      | note-title        |
      | Lord of the Rings |
      | Harry Potter      |
      | Art of War        |

  @ignore
  Scenario: Add note with details to notebook
    Given I have a notebook with the head note "Lord of the Rings"
    When AI agent calls the "add_note" MCP tool with notebook title "Lord of the Rings" and title "Sam" and details "holdo"
    Then I should see the note tree in the sidebar
      | note-title        |
      | Lord of the Rings |
      | Sam               |
    And I should see "Lord of the Rings" with these children
      | note-title        |
      | Lord of the Rings |
      | Sam               |

  @ignore
  Scenario: Add note to user select notebook
    Given I have a notebook with the head note "Lord of the Rings"
    And I have a notebook with the head note "Harry Potter"
    When I add a note with title "Sam" and details "holdo" without notebook title
    Then AI agent calls the "get_notebook_list" MCP tool and show me the notebook list
    And AI agent calls the "add_note" MCP tool with notebook title "Lord of the Rings" and title "Sam" and details "holdo"
    Then I should see the note tree in the sidebar
      | note-title        |
      | Lord of the Rings |
      | Sam               |
    And I should see "Lord of the Rings" with these children
      | note-title        |
      | Lord of the Rings |
      | Sam               |

  Scenario Outline: AI developer learns from Doughnut via MCP (happy case)
    Given I have a notebook with the head note "Lord of the Rings" and details "Test"
    And I have a notebook with the head note "Harry Potter" and details "Harry Potter is handsome"
    When I search for notes with the term "<search_term>" 
    Then the response should contain "<note_title>"

    Examples:
      | search_term | note_title           |
      | Lord        | Lord of the Rings    |
      | Harry       | Harry Potter         |

  Scenario Outline: AI developer learns from Doughnut via MCP (unhappy case)
    Given I have a notebook with the head note "Lord of the Rings" and details "Test"
    And I have a notebook with the head note "Harry Potter" and details "Harry Potter is handsome"
    When I search for notes with the term "<search_term>"
    Then the response should contain "No relevant note found."

    Examples:
      | search_term |
      | Frodo       |
      | Hermione    |
