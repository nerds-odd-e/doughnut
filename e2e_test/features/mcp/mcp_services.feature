@ConnectAndDisconnectHttpsMcpWhenTeardown
Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Donut, so that AI can search, read, and explore my notes as evidence for the
  knowledge journey.

  Background:
    Given I am logged in as "old_learner"
    And I have a valid Donut Access Token with label "For MCP services"
    And I connect to an MCP client that connects to Donut MCP service
    And the MCP client has discovered the note context tools
    And I have a notebook "CS concepts" with notes:
      | Title           | Folder               |
      | Object Oriented | Programming Concepts |
      | Functional      | Programming Concepts |

  @mcpIsolationProof
  Scenario: Isolated MCP search stays in this worktree
    Given I add this worktree's MCP isolation note
    And I wait until the other worktree has seeded its MCP isolation note
    Then the MCP client used this checkout's isolated HTTPS MCP endpoint
    When AI agent searches for this worktree's MCP isolation marker
    Then the response should contain this worktree's MCP isolation marker
    When AI agent extracts note ID and calls get graph MCP tool with token limit "1000"
    Then the graph response should contain this worktree's MCP isolation focus note
    And the graph response should not contain the other worktree's MCP isolation marker
    When AI agent searches for the other worktree's MCP isolation marker
    Then the response should contain "No matching notes."
    When the MCP client disconnects

  Scenario: AI agent reports no match for an unmatched MCP search
    When AI agent searches for relevant notes using MCP tool with the term "Fiona"
    Then the response should contain "No matching notes."

  Scenario: AI agent lists every matching note as a candidate
    Given I have a notebook "Maths" with notes:
      | Title               | Folder   |
      | Functional analysis | Analysis |
    When AI agent searches for relevant notes using MCP tool with the term "Functional"
    Then the note candidates should be "Functional, Functional analysis"

  Scenario: Equal-title candidates are told apart by notebook and folder
    Given I have a notebook "Maths" with notes:
      | Title      |
      | Functional |
    When AI agent searches for relevant notes using MCP tool with the term "Functional"
    Then equal-title note candidates are disambiguated by notebook and folder

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

  Scenario: AI agent reads a candidate note and cites its notebook revision
    Given note "Functional" has content:
      """
      Pure functions
      """
    When AI agent searches for relevant notes using MCP tool with the term "Functional"
    And AI agent reads the first candidate note
    Then the read note content should contain "Pure functions" with an accepted revision to cite
    When I update note "Functional" with content "Functions without side effects"
    And AI agent reads the first candidate note
    Then the read note should cite a different accepted revision

  Scenario: MCP calls stay scoped to each personal token's user
    Given there are some notes for existing user "another_old_learner" in notebook "Other Private"
      | Title          |
      | BetaOnlySecret |
    And I have a notebook "My Private" with notes:
      | Title           |
      | AlphaOnlySecret |
    When AI agent searches for relevant notes using MCP tool with the term "BetaOnlySecret"
    Then the response should contain "No matching notes."
    Given I am re-logged in as "another_old_learner"
    And I have a valid Donut Access Token with label "Other MCP services"
    And I connect to an MCP client that connects to Donut MCP service
    When AI agent searches for relevant notes using MCP tool with the term "BetaOnlySecret"
    Then the response should contain "BetaOnlySecret"
    When AI agent searches for relevant notes using MCP tool with the term "AlphaOnlySecret"
    Then the response should contain "No matching notes."
