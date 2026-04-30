Feature: Wiki links in notes
  As a learner, I want wiki-style links in my note content so I can open related notes,
  add a note when a link has no target, and see unresolved links clearly.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "WikiLinks E2E NB" with a note "WikiLinks E2E Root" and details "An awesome training"
    And there are some notes:
      | Title             | Parent Title       |
      | WikiLinks E2E Tech | WikiLinks E2E Root |
      | WikiLinks E2E CI   | WikiLinks E2E Root |

  Scenario: A wiki link points to the note with the same title
    When I update note "WikiLinks E2E Tech" details using markdown to become:
      """
      Technical excellence means supporting [[WikiLinks E2E CI]].
      """
    Then I should see the rich content of the note with details:
      | Tag | Content          |
      | a   | WikiLinks E2E CI |
    And the link "WikiLinks E2E CI" should link to the note with the same title

  Scenario: A qualified wiki link opens a note in another notebook
    Given I have a notebook "WikiCross Src NB" with a note "WikiCross From" and details "origin"
    And I have a notebook "WikiCross Tgt NB" with a note "WikiCross Tgt Root" and notes:
      | Title           | Parent Title      |
      | WikiCross Deep  | WikiCross Tgt Root |
    When I update note "WikiCross From" details using markdown to become:
      """
      Read [[WikiCross Tgt NB:WikiCross Deep]].
      """
    Then I should see the rich content of the note with details:
      | Tag | Content                            |
      | a   | WikiCross Tgt NB:WikiCross Deep    |
    And the link "WikiCross Tgt NB:WikiCross Deep" should open the note titled "WikiCross Deep"

  Scenario: A dead wiki link is shown and can create the missing note
    When I update note "WikiLinks E2E CI" details using markdown to become:
      """
      Continuous integration is distinct from a [[WikiLinks E2E Missing]].
      We also rely on [[WikiLinks E2E Tech]] as a core practice.
      """
    Then I should see the rich content of the note with details:
      | Tag               | Content            |
      | a.dead-link       | WikiLinks E2E Missing |
      | a:not(.dead-link) | WikiLinks E2E Tech |
    And I should be able to create a new note by following the dead link "WikiLinks E2E Missing"
    And I should see "WikiLinks E2E NB/WikiLinks E2E Root" with these children
      | note-title             |
      | WikiLinks E2E Tech     |
      | WikiLinks E2E CI      |
      | WikiLinks E2E Missing |
