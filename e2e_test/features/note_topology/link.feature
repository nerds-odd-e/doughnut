Feature: Wiki links in notes
  As a learner, I want wiki-style links in my note content so I can open related notes,
  add a note when a link has no target, and see unresolved links clearly.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with a note "LeSS in Action" and details "An awesome training"
    And there are some notes:
      | Title                  | Parent Title   |
      | Technical Excellence   | LeSS in Action |
      | Continuous Integration | LeSS in Action |

  Scenario: A wiki link points to the note with the same title
    When I update note "Technical Excellence" details using markdown to become:
      """
      Technical excellence means supporting [[Continuous Integration]].
      """
    Then I should see the rich content of the note with details:
      | Tag | Content                |
      | a   | Continuous Integration |
    And the link "Continuous Integration" should link to the note with the same title

  Scenario: A dead wiki link is shown and can create the missing note
    When I update note "Continuous Integration" details using markdown to become:
      """
      Continuous integration is distinct from a [[Continuous Integration System]].
      We also rely on [[Technical Excellence]] as a core practice.
      """
    Then I should see the rich content of the note with details:
      | Tag               | Content                       |
      | a.dead-link       | Continuous Integration System |
      | a:not(.dead-link) | Technical Excellence          |
    And I should be able to create a new note by following the dead link "Continuous Integration System"
    And I should see "LeSS training/LeSS in Action" with these children
      | note-title                    |
      | Technical Excellence          |
      | Continuous Integration        |
      | Continuous Integration System |
