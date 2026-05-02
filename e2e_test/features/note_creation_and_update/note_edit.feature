Feature: Note Edit
  As a learner, I want to edit and undo editing for single user,
  with title and details only within a session.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with a note "LeSS in Action" and details "An awesome training"

  Scenario: Edit a note title and edit details and undo
    Given I update note title "LeSS in Action" to become "Odd-e CSD"
    And the note title should be "Odd-e CSD"
    And I update note "Odd-e CSD" details from "An awesome training" to become "A super awesome training"
    And the note details should include "A super awesome training"
    When I undo "edit details"
    Then the note details should include "An awesome training"
    When I undo "edit title" again
    Then the note title should be "LeSS in Action"
    And there should be no more undo to do

  Scenario: Edit a note details with bullet points
    When I update note "LeSS in Action" to become:
      | Title     | Details     |
      | Odd-e CSD | * must join |
    Then the note details should include "must join"

  Scenario: Edit a note's details as markdown
    When I update note "LeSS in Action" details using markdown to become:
      """
      # Odd-e LiA
      ## Our best training

      * Specification by Example
        * Discuss in workshop
        * Conccurent engineering
        * Living documentation
      * Test-Driven Development
      """
    Then I should see the rich content of the note with details:
      | Tag            | Content                  |
      | h1             | Odd-e LiA                |
      | h2             | Our best training        |
      | li             | Specification by Example |
      | li.ql-indent-1 | Living documentation     |

  Scenario: Edit a note's details with a markdown table
    When I update note "LeSS in Action" details using markdown to become:
      """
      | Name    | Score |
      | ------- | ----- |
      | Alice   |  95   |
      | Bob     |  88   |
      """
    Then I should see the rich content of the note with details:
      | Tag    | Content |
      | table  |         |
      | thead  |         |
      | tr     |         |
      | th     | Name    |
      | th     | Score   |
      | tbody  |         |
      | tr     |         |
      | td     | Alice   |
      | td     | 95      |
      | tr     |         |
      | td     | Bob     |
      | td     | 88      |

  Scenario: Edit a note title should update the sidebar
    Given I have a notebook "LeSS training" with notes:
      | Title | Parent Title   |
      | TDD   | LeSS in Action |
    When I update note title "TDD" to become "Critical thinking"
    Then I should see the note tree in the sidebar
      | note-title        |
      | LeSS in Action    |
      | Critical thinking |

  Scenario: Edit a note details with Shift-Enter adds line break
    When I update note "LeSS in Action" to become:
      | Title     | Details              |
      | Odd-e CSD | Hello<Shift-Enter>World |
    Then the note details should include "Hello"
    And the note details should include "World"
    And the note details should contain a line break

  Scenario: Note YAML properties round-trip through markdown and rich editing
    When I update note "LeSS in Action" details using markdown to become:
      """
      ---
      diligence: high
      topic: training
      ---

      # Workshop Body
      Main content here.
      """
    And I flush pending note details save
    And I reload the current page for note "LeSS in Action"
    And I open the note details markdown editor
    Then the note details markdown source should contain "diligence: high"
    And the note details markdown source should contain "topic: training"
    When I view the note details as rich content
    And I should see the rich content elements in the note details:
      | Tag | Content       |
      | h1  | Workshop Body |
    When I add a rich note property with key "status" and value "draft"
    And I flush pending note details save
    And I reload the current page for note "LeSS in Action"
    Then I should see rich note property "status" with value "draft"
    When I edit the rich note property with key "topic" to key "domain" and value "wiki"
    And I flush pending note details save
    And I reload the current page for note "LeSS in Action"
    Then I should not see rich note property "topic"
    And I should see rich note property "domain" with value "wiki"
    And I should see rich note property "status" with value "draft"
    When I open the note details markdown editor
    Then the note details markdown source should contain "domain: wiki"
    And the note details markdown source should contain "diligence: high"
    And the note details markdown source should contain "status: draft"
    And the note details markdown source should not contain "topic: training"
