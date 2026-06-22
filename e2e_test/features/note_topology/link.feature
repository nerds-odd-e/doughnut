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

  @mockBrowserTime
  Scenario: Moving a note across notebooks keeps outgoing links pointed at the old notebook
    Given I have a notebook "WikiMove Old NB" with notes:
      | Title            | Content                                                                | Folder            |
      | WikiMove Target  | old notebook target                                                    | WikiMove Old Root |
      | WikiMove Carrier | Read [[WikiMove Target]] and [[WikiMove Other NB:WikiMove Qualified]]. | WikiMove Old Root |
    And I have a notebook "WikiMove Other NB" with a note "WikiMove Qualified" and content "qualified notebook target"
    And I have a notebook "WikiMove New NB" with a note "WikiMove Target" and content "new notebook target"
    When I route to the note "WikiMove Carrier"
    And I move the current note to notebook "WikiMove New NB" root via the link toolbar
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiMove Old NB:WikiMove Target|WikiMove Target]]"
    And the note content markdown source should contain "[[WikiMove Other NB:WikiMove Qualified]]"
    When I view the note content as rich content
    Then the link "WikiMove Target" should open the note titled "WikiMove Target"
    And the note content on the current page should be "old notebook target"

  @mockBrowserTime
  Scenario: Moving a note into a folder across notebooks keeps inbound and outgoing links correct
    Given I have a notebook "WikiFolderMove Old NB" with notes:
      | Title                  | Content                                  | Folder                  |
      | WikiFolderMove Target  | old notebook target                      | WikiFolderMove Old Root |
      | WikiFolderMove Carrier | Read [[WikiFolderMove Target]].          | WikiFolderMove Old Root |
      | WikiFolderMove Ref     | See [[WikiFolderMove Carrier]].          | WikiFolderMove Old Root |
    And I have a notebook "WikiFolderMove New NB" with notes:
      | Title                 | Content             | Folder                |
      | WikiFolderMove Target | new notebook target | WikiFolderMove Folder |
    When I route to the note "WikiFolderMove Carrier"
    And I move the current note under folder "WikiFolderMove Folder" in notebook "WikiFolderMove New NB" via the link toolbar
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiFolderMove Old NB:WikiFolderMove Target|WikiFolderMove Target]]"
    When I view the note content as rich content
    Then the link "WikiFolderMove Target" should open the note titled "WikiFolderMove Target"
    And the note content on the current page should be "old notebook target"
    When I route to the note "WikiFolderMove Ref"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiFolderMove New NB:WikiFolderMove Carrier|WikiFolderMove Carrier]]"
    When I view the note content as rich content
    Then the link "WikiFolderMove Carrier" should open the note titled "WikiFolderMove Carrier"

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
    When I route to the note "WikiLinks E2E CI"
    When I set the note title to "WikiLinks E2E CI Renamed" keeping visible reference text
    When I route to the note "WikiLinks E2E Tech"
    Then I should see the rich content of the note with content:
      | Tag | Content          |
      | a   | WikiLinks E2E CI |
    And the link "WikiLinks E2E CI" should open the note titled "WikiLinks E2E CI Renamed"

  Scenario: Renaming a referenced note while updating visible reference text
    When I update note "WikiLinks E2E Tech" content using markdown to become:
      """
      See [[WikiLinks E2E CI]] for process.
      """
    When I route to the note "WikiLinks E2E CI"
    When I set the note title to "WikiLinks E2E CI Renamed" updating visible reference text
    When I route to the note "WikiLinks E2E Tech"
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
