Feature: Wiki links in notes
  As a learner, I want wiki-style links in my note content so I can open related notes,
  add a note when a link has no target, and see unresolved links clearly,
  and insert wiki links via the toolbar.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "WikiLinks E2E NB" with notes:
      | Title              | Content | Folder             |
      | WikiLinks E2E Tech |                     | WikiLinks E2E Root |
      | WikiLinks E2E CI   |                     | WikiLinks E2E Root |

  Scenario: A wiki link points to the note with the same title
    When I update note "WikiLinks E2E Tech" content using markdown to become:
      """
      Technical excellence means supporting [[WikiLinks E2E CI]].
      """
    Then I should see the rich content of the note with content:
      | Tag | Content          |
      | a   | WikiLinks E2E CI |
    And the link "WikiLinks E2E CI" should link to the note with the same title

  Scenario: A qualified wiki link opens a note in another notebook
    Given I have a notebook "WikiCross Src NB" with a note "WikiCross From" and content "origin"
    And I have a notebook "WikiCross Tgt NB" with notes:
      | Title           | Folder            |
      | WikiCross Deep  | WikiCross Tgt Root |
    When I update note "WikiCross From" content using markdown to become:
      """
      Read [[WikiCross Tgt NB:WikiCross Deep]].
      """
    Then I should see the rich content of the note with content:
      | Tag | Content                            |
      | a   | WikiCross Tgt NB:WikiCross Deep    |
    And the link "WikiCross Tgt NB:WikiCross Deep" should open the note titled "WikiCross Deep"

  Scenario: A dead wiki link is shown and can create the missing note
    When I update note "WikiLinks E2E CI" content using markdown to become:
      """
      Continuous integration is distinct from a [[WikiLinks E2E Missing]].
      """
    And I should be able to create a new note by following the dead link "WikiLinks E2E Missing"
    Then note "WikiLinks E2E CI" should show the rich content elements in the note content:
      | Tag               | Content               |
      | a:not(.dead-link) | WikiLinks E2E Missing |

  @mockBrowserTime
  Scenario: A dead wiki link can be relinked to an existing note
    When I update note "WikiLinks E2E CI" content using markdown to become:
      """
      Continuous integration relies on [[original text]].
      """
    Then I should see the rich content of the note with content:
      | Tag         | Content       |
      | a.dead-link | original text |
    When I link dead link "original text" to existing note "WikiLinks E2E Tech"
    Then I should see the rich content of the note with content:
      | Tag               | Content       |
      | a:not(.dead-link) | original text |
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiLinks E2E Tech|original text]]"

  @mockBrowserTime
  Scenario: Insert a wiki link to a note in the same notebook via the toolbar
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E Tech" note
    And I insert a wiki link to "WikiLinks E2E CI" via the link toolbar
    Then I should see the rich content elements in the note content:
      | Tag | Content          |
      | a   | WikiLinks E2E CI |
    And the link "WikiLinks E2E CI" should link to the note with the same title

  Scenario: Renaming a referenced note while keeping visible reference text
    When I update note "WikiLinks E2E Tech" content using markdown to become:
      """
      See [[WikiLinks E2E CI]] for process.
      """
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E CI" note
    When I set the note title to "WikiLinks E2E CI Renamed" keeping visible reference text
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E Tech" note
    Then I should see the rich content of the note with content:
      | Tag | Content          |
      | a   | WikiLinks E2E CI |
    And the link "WikiLinks E2E CI" should open the note titled "WikiLinks E2E CI Renamed"

  Scenario: Renaming a referenced note while updating visible reference text
    When I update note "WikiLinks E2E Tech" content using markdown to become:
      """
      See [[WikiLinks E2E CI]] for process.
      """
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E CI" note
    When I set the note title to "WikiLinks E2E CI Renamed" updating visible reference text
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E Tech" note
    Then I should see the rich content of the note with content:
      | Tag | Content                    |
      | a   | WikiLinks E2E CI Renamed   |
    And the link "WikiLinks E2E CI Renamed" should open the note titled "WikiLinks E2E CI Renamed"

  @mockBrowserTime
  Scenario: Insert a qualified wiki link to a note in another notebook via the toolbar
    Given I have a notebook "WikiCross Tgt NB" with notes:
      | Title          | Folder             |
      | WikiCross Deep | WikiCross Tgt Root |
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E Tech" note
    And I insert a wiki link to "WikiCross Deep" via the link toolbar
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiCross Tgt NB:WikiCross Deep]]"
