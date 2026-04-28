Feature: Note wiki links
  As a learner, I want wiki-style links in note details to connect to other notes,
  create missing notes from dead links, and see unresolved links clearly.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "LeSS in Action" and details "An awesome training"
    And there are some notes:
      | Title                  | Parent Title   |
      | Technical Excellence   | LeSS in Action |
      | Continuous Integration | LeSS in Action |

  Scenario: Edit a note's details with a wiki link in markdown
    When I update note "Technical Excellence" details using markdown to become:
      """
      Technical excellence is to support [[Continuous Integration]].
      """
    Then I should see the rich content of the note with details:
      | Tag | Content                |
      | a   | Continuous Integration |
    And the link "Continuous Integration" should link to the note with the same title

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
