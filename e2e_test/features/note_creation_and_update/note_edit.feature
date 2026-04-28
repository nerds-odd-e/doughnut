Feature: Note Edit
  As a learner, I want to edit and undo editing for single user,
  with title and details only within a session.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "LeSS in Action" and details "An awesome training"

  Scenario: Edit a note
    And I update note "LeSS in Action" to become:
      | Title     | Details           |
      | Odd-e CSD | Our best training |
    Then I should see "Odd-e CSD" in the page
    And I should see these notes belonging to the user at the top level of all my notes
      | Title     | Details           |
      | Odd-e CSD | Our best training |

  Scenario: Edit a note title and edit details and undo
    Given I update note title "LeSS in Action" to become "Odd-e CSD"
    And I should see "Odd-e CSD" in the page
    And I update note "Odd-e CSD" details from "An awesome training" to become "A super awesome training"
    And I should see "A super awesome training" in the page
    When I undo "edit details"
    Then I should see "An awesome training" in the page
    When I undo "edit title" again
    Then I should see "LeSS in Action" in the page
    And there should be no more undo to do

  Scenario: Edit a note details with bullet points
    When I update note "LeSS in Action" to become:
      | Title     | Details     |
      | Odd-e CSD | * must join |
    Then I should see "must join" in the page

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
    Given there are some notes:
      | Title | Parent Title   |
      | TDD   | LeSS in Action |
    When I update note title "TDD" to become "Critical thinking"
    Then I should see the note tree in the sidebar
      | note-title        |
      | Critical thinking |

  Scenario: Edit a note details with Shift-Enter adds line break
    When I update note "LeSS in Action" to become:
      | Title     | Details              |
      | Odd-e CSD | Hello<Shift-Enter>World |
    Then I should see "Hello" in the page
    And I should see "World" in the page
    And the note details should contain a line break

  Scenario: Edit a note's details with a wiki link in markdown
    Given I have a notebook with the head note "TDD"
    And there are some notes:
      | Title          | Parent Title |
      | hoge fuga piyo | TDD          |
    When I update note "TDD" details using markdown to become:
      """
      [[hoge fuga piyo]]
      """
    Then I should see the rich content of the note with details:
      | Tag | Content        |
      | a   | hoge fuga piyo |
    When I click the link "hoge fuga piyo" in the note details
    Then I should be on the note page of "hoge fuga piyo"

  Scenario: Edit a note's details with a dead wiki link in markdown
    Given I have a notebook with the head note "TDD"
    And there are some notes:
      | Title          | Parent Title |
      | hoge fuga piyo | TDD          |
    When I update note "TDD" details using markdown to become:
      """
      [[foo bar]]
      """
    Then I should see the rich content of the note with details:
      | Tag         | Content |
      | a.dead-link | foo bar |

  Scenario: Clicking dead wiki link opens note creation form with pre-filled title
    Given I have a note that includes deadlink [[new concept]]
    When I click the dead link "new concept" in the note details
    Then I should see a note creation form
    And the title is "new concept" pre-filled

  Scenario: Creating note from dead wiki link navigates to the new note
    Given I have a note that includes deadlink [[new concept]]
    When I create the note [[new concept]] from the dead link dialog
    Then I should see "new concept" in the page
