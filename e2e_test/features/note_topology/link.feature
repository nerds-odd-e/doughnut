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
    When I update note "Continuous Integration" details using markdown to become:
      """
      Continuous Integration is different from the [[Continuous Integration System]],
      which is a good practice in [[Technical Excellence]].
      """
    Then I should see the rich content of the note with details:
      | Tag         | Content |
      | a.dead-link | Continuous Integration System |
    When I create the note [[Continuous Integration System]] from the dead link dialog
    Then I should see "Continuous Integration System" in the page
