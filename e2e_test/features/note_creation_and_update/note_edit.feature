Feature: Note Edit
  As a learner, I want to edit and undo editing for single user,
  with title and content only within a session.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with a note "LeSS in Action" and content "Before"

  Scenario: Edit a note title and edit content and undo
    Given I update note title "LeSS in Action" to become "Odd-e CSD"
    And I update note "Odd-e CSD" content to become "After"
    When I undo "edit content"
    Then the note content should include "Before"
    When I undo "edit title" again
    Then the note title should be "LeSS in Action"
    And there should be no more undo to do

  Scenario: Edit note content with bullet points
    When I update note "LeSS in Action" to become:
      | Title     | Content |
      | Odd-e CSD | * must join |
    Then the note content should include "must join"

  Scenario: Edit a note's content as markdown
    When I update note "LeSS in Action" content using markdown to become:
      """
      # Odd-e LiA
      ## Our best training

      * Specification by Example
        * Discuss in workshop
        * Conccurent engineering
        * Living documentation
      * Test-Driven Development
      """
    Then I should see the rich content of the note with content:
      | Tag            | Content                  |
      | h1             | Odd-e LiA                |
      | li             | Specification by Example |
      | li.ql-indent-1 | Living documentation     |

  Scenario: Edit a note's content with a markdown table
    When I update note "LeSS in Action" content using markdown to become:
      """
      | Name    | Score |
      | ------- | ----- |
      | Alice   |  95   |
      | Bob     |  88   |
      """
    Then I should see the rich content of the note with content:
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
      | Title |
      | TDD   |
    When I update note title "TDD" to become "Critical thinking"
    Then I should see the note tree in the sidebar
      | note-title        |
      | Critical thinking |
      | LeSS in Action    |

  Scenario: Edit note content with Shift-Enter adds line break
    When I update note "LeSS in Action" to become:
      | Title     | Content |
      | Odd-e CSD | Hello<Shift-Enter>World |
    Then the note content should include "Hello"
    And the note content should include "World"
    And the note content should contain a line break

  Scenario: Note YAML properties round-trip through markdown and rich editing
    Given note "LeSS in Action" has content:
      """
      ---
      diligence: high
      topic: training
      ---

      # Workshop Body
      Main content here.
      """
    When I visit note "LeSS in Action"
    Then I should see rich note property "diligence" with value "high"
    And I should see rich note property "topic" with value "training"
    When I add a rich note property with key "status" and value "draft"
    And I edit the rich note property with key "topic" to key "domain" and value "wiki"
    And I reload the current page for note "LeSS in Action"
    Then I should see rich note property "status" with value "draft"
    And I should not see rich note property "topic"
    And I should see rich note property "domain" with value "wiki"
    And I should see rich note property "diligence" with value "high"
    When I open the note content markdown editor
    Then the note content markdown source should contain "domain: wiki"
    And the note content markdown source should contain "diligence: high"
    And the note content markdown source should contain "status: draft"
    And the note content markdown source should not contain "topic: training"
