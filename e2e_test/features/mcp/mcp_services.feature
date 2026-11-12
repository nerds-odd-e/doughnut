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
    When AI agent follows the candidate path to get graph MCP tool with token limit "1000"
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
    When AI agent follows the candidate path to get graph MCP tool with token limit "<token_limit>"
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

  Scenario: AI agent recovers an ambiguous read by selecting a returned path
    Given I have a note "Code Review" under notebook "Engineering" in folder "Practices/Design" with content:
      """
      Design review guidance
      """
    And I have a note "Code Review" under notebook "Engineering" in folder "Other/Design" with content:
      """
      Other review guidance
      """
    When AI agent reads "Engineering:Code Review" and selects the candidate in "Practices/Design"
    Then the read note should be at "Engineering:Practices/Design/Code Review" in folder "Practices / Design"

  @disableOpenAiService
  Scenario: AI agent reads a wrong spelling answer from recall
    Given I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | sedition | Sedition means incite violence |
    And the note "sedition" was assimilated as spelling on day 1
    When I visit recall for a due recall prompt on day 2
    And I type my answer "sedation"
    Then I should see that my spelling answer "sedation" is incorrect
    When AI agent lists my recall answers
    Then the recall answers should include the spelling answer "sedation" to a note containing "Sedition means incite violence"

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
