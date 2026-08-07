Feature: Note Edit
  As a learner, I want to edit and undo editing for single user,
  with title and content only within a session.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with a note "LeSS in Action" and content "Before"

  Scenario: Undo content edit restores previous content
    Given I update note title "LeSS in Action" to become "Odd-e CSD"
    And I update note "Odd-e CSD" content to become "After"
    When I undo "edit content"
    Then the note content should include "Before"

  Scenario: Undo title edit restores previous title
    Given I update note title "LeSS in Action" to become "Odd-e CSD"
    And I update note "Odd-e CSD" content to become "After"
    And I undo "edit content"
    When I undo "edit title"
    Then the note title should be "LeSS in Action"
    And there should be nothing left to undo

  Scenario: Edit note content with bullet points
    When I update note "LeSS in Action" to become:
      | Title     | Content    |
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
    Then I should see the note content rendered as:
      | Kind               | Text                     |
      | heading 1          | Odd-e LiA                |
      | list item          | Specification by Example |
      | indented list item | Living documentation     |

  Scenario: Edit a note's content with a markdown table
    When I update note "LeSS in Action" content using markdown to become:
      """
      | Name    | Score |
      | ------- | ----- |
      | Alice   |  95   |
      | Bob     |  88   |
      """
    Then I should see the note content rendered as:
      | Kind         | Text |
      | table        |      |
      | table header | Name |
      | table header | Score |
      | table cell   | Alice |
      | table cell   | 95    |
      | table cell   | Bob   |
      | table cell   | 88    |

  Scenario: Edit a note title should update the sidebar
    Given I have a notebook "LeSS training" with notes:
      | Title |
      | TDD   |
    When I update note title "TDD" to become "Critical thinking"
    Then I should see the note tree in the sidebar
      | note-title        |
      | Critical thinking |
      | LeSS in Action    |

  Scenario: Soft line break in note content
    When I insert a soft line break in note "LeSS in Action" between "Hello" and "World"
    Then the note content should include "Hello"
    And the note content should include "World"
    And the note content should contain a soft line break between "Hello" and "World"

  Scenario: YAML frontmatter appears as rich note properties
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

  Scenario: Rich note property edits persist after reload
    Given note "LeSS in Action" has content:
      """
      ---
      diligence: high
      topic: training
      ---

      # Workshop Body
      Main content here.
      """
    And I visit note "LeSS in Action"
    When I add a rich note property with key "status" and value "draft"
    And I edit the rich note property with key "topic" to key "domain" and value "wiki"
    And I reload the current page for note "LeSS in Action"
    Then I should see rich note property "status" with value "draft"
    And I should not see rich note property "topic"
    And I should see rich note property "domain" with value "wiki"
    And I should see rich note property "diligence" with value "high"

  Scenario: Markdown source reflects rich note property edits
    Given note "LeSS in Action" has content:
      """
      ---
      diligence: high
      topic: training
      ---

      # Workshop Body
      Main content here.
      """
    And I visit note "LeSS in Action"
    And I add a rich note property with key "status" and value "draft"
    And I edit the rich note property with key "topic" to key "domain" and value "wiki"
    When I open the note content markdown editor
    Then the note content markdown source should contain "domain: wiki"
    And the note content markdown source should contain "diligence: high"
    And the note content markdown source should contain "status: draft"
    And the note content markdown source should not contain "topic: training"
