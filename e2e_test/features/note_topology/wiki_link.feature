Feature: Wiki links in notes
  As a learner, I want wiki-style links in my note content so I can open related notes,
  add a note when a wiki link has no target, see unresolved wiki links clearly,
  and insert wiki links while editing.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "WikiLinks E2E NB" with notes:
      | Title              | Folder             |
      | WikiLinks E2E Tech | WikiLinks E2E Root |
      | WikiLinks E2E CI   | WikiLinks E2E Root |

  Scenario: A folder-path wiki link opens the note in that folder
    Given I have a notebook "WikiPath Folder NB" with notes:
      | Title            | Content          | Folder           |
      | WikiPath Shared  | recipes namesake | WikiPath Recipes |
      | WikiPath Carrier | origin           | WikiPath Root    |
    And I have a notebook "WikiPath Folder NB" with notes:
      | Title           | Content         | Folder          |
      | WikiPath Shared | pantry namesake | WikiPath Pantry |
    When I update note "WikiPath Carrier" content using markdown to become:
      """
      See [[WikiPath Pantry/WikiPath Shared]].
      """
    Then I should see the note content rendered as:
      | Kind           | Text                                 |
      | live wiki link | WikiPath Pantry/WikiPath Shared      |
    And the wiki link "WikiPath Pantry/WikiPath Shared" should open the note titled "WikiPath Shared"
    And the note content on the current page should be "pantry namesake"

  Scenario Outline: A path Markdown link opens like a wiki link and keeps its spelling
    Given I have a notebook "WikiPathMdNB" with notes:
      | Title             | Content         | Folder           |
      | WikiPathMdTitle   | folder namesake | WikiPathMdFolder |
      | WikiPathMdRoot    | root namesake   |                  |
      | WikiPathMdCarrier | origin          | WikiPathMdFolder |
    When I update note "WikiPathMdCarrier" content using markdown to become:
      """
      See <markdown>.
      """
    Then I should see the note content rendered as:
      | Kind           | Text      |
      | live wiki link | <display> |
    When I view the note content as markdown
    Then the note content markdown source should contain "<markdown>"
    And the note content markdown source should not contain "[[<display>]]"
    When I view the note content as rich content
    Then following the wiki link "<display>" should open the note titled "<target_title>"
    And the note content on the current page should be "<target_content>"

    Examples:
      | markdown                                      | display | target_title    | target_content  |
      | [label](/WikiPathMdFolder/WikiPathMdTitle.md) | label   | WikiPathMdTitle | folder namesake |
      | [label](/WikiPathMdFolder/WikiPathMdTitle)    | label   | WikiPathMdTitle | folder namesake |
      | [label](/WikiPathMdRoot.md)                   | label   | WikiPathMdRoot  | root namesake   |

  Scenario: A nested folder-path wiki link opens the nested note
    Given I have a notebook "WikiPath Nested NB" with notes:
      | Title                   | Content         | Folder                                       |
      | WikiPath Nested Shared  | nested namesake | WikiPath Nested Parent/WikiPath Nested Child |
      | WikiPath Nested Carrier | origin          | WikiPath Nested Root                         |
    And I have a notebook "WikiPath Nested NB" with notes:
      | Title                  | Content          | Folder                 |
      | WikiPath Nested Shared | shallow namesake | WikiPath Nested Parent |
    When I update note "WikiPath Nested Carrier" content using markdown to become:
      """
      See [[WikiPath Nested Parent/WikiPath Nested Child/WikiPath Nested Shared]].
      """
    Then I should see the note content rendered as:
      | Kind           | Text                                                            |
      | live wiki link | WikiPath Nested Parent/WikiPath Nested Child/WikiPath Nested Shared |
    And the wiki link "WikiPath Nested Parent/WikiPath Nested Child/WikiPath Nested Shared" should open the note titled "WikiPath Nested Shared"
    And the note content on the current page should be "nested namesake"

  Scenario: A wiki link points to the note with the same title
    When I update note "WikiLinks E2E Tech" content using markdown to become:
      """
      Technical excellence means supporting [[WikiLinks E2E CI]].
      """
    Then I should see the note content rendered as:
      | Kind      | Text             |
      | wiki link | WikiLinks E2E CI |
    And the wiki link "WikiLinks E2E CI" should link to the note with the same title

  Scenario: A qualified wiki link opens a note in another notebook
    Given I have a notebook "WikiCross Src NB" with a note "WikiCross From" and content "origin"
    And I have a notebook "WikiCross Tgt NB" with notes:
      | Title          | Folder             |
      | WikiCross Deep | WikiCross Tgt Root |
    When I update note "WikiCross From" content using markdown to become:
      """
      Read [[WikiCross Tgt NB:WikiCross Deep]].
      """
    Then I should see the note content rendered as:
      | Kind      | Text                            |
      | wiki link | WikiCross Tgt NB:WikiCross Deep |
    And the wiki link "WikiCross Tgt NB:WikiCross Deep" should open the note titled "WikiCross Deep"

  @mockBrowserTime
  Scenario: Moving a note across notebooks keeps outgoing wiki links pointed at the old notebook
    Given I have a notebook "WikiMove Old NB" with notes:
      | Title            | Content                                                                | Folder            |
      | WikiMove Target  | old notebook target                                                    | WikiMove Old Root |
      | WikiMove Carrier | Read [[WikiMove Target]] and [[WikiMove Other NB:WikiMove Qualified]]. | WikiMove Old Root |
    And I have a notebook "WikiMove Other NB" with a note "WikiMove Qualified" and content "qualified notebook target"
    And I have a notebook "WikiMove New NB" with a note "WikiMove Target" and content "new notebook target"
    When I route to the note "WikiMove Carrier"
    And I move the current note to notebook "WikiMove New NB" root
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiMove Old NB:WikiMove Target|WikiMove Target]]"
    And the note content markdown source should contain "[[WikiMove Other NB:WikiMove Qualified]]"
    When I view the note content as rich content
    Then the wiki link "WikiMove Target" should open the note titled "WikiMove Target"
    And the note content on the current page should be "old notebook target"

  @mockBrowserTime
  Scenario: Moving a note into a folder across notebooks keeps inbound and outgoing wiki links correct
    Given I have a notebook "WikiFolderMove Old NB" with notes:
      | Title                  | Content                         | Folder                  |
      | WikiFolderMove Target  | old notebook target             | WikiFolderMove Old Root |
      | WikiFolderMove Carrier | Read [[WikiFolderMove Target]]. | WikiFolderMove Old Root |
      | WikiFolderMove Ref     | See [[WikiFolderMove Carrier]]. | WikiFolderMove Old Root |
    And I have a notebook "WikiFolderMove New NB" with notes:
      | Title                 | Content             | Folder                |
      | WikiFolderMove Target | new notebook target | WikiFolderMove Folder |
    When I route to the note "WikiFolderMove Carrier"
    And I move the current note under folder "WikiFolderMove Folder" in notebook "WikiFolderMove New NB"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiFolderMove Old NB:WikiFolderMove Target|WikiFolderMove Target]]"
    When I view the note content as rich content
    Then the wiki link "WikiFolderMove Target" should open the note titled "WikiFolderMove Target"
    And the note content on the current page should be "old notebook target"
    When I route to the note "WikiFolderMove Ref"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiFolderMove New NB:WikiFolderMove Carrier|WikiFolderMove Carrier]]"
    When I view the note content as rich content
    Then the wiki link "WikiFolderMove Carrier" should open the note titled "WikiFolderMove Carrier"

  Scenario: A dead wiki link is shown and can create the missing note
    When I update note "WikiLinks E2E CI" content using markdown to become:
      """
      Continuous integration is distinct from a [[WikiLinks E2E Missing]].
      """
    And I create a new note by following the dead wiki link "WikiLinks E2E Missing"
    Then note "WikiLinks E2E CI" should show the note content rendered as:
      | Kind           | Text                  |
      | live wiki link | WikiLinks E2E Missing |

  @mockBrowserTime
  Scenario: A dead wiki link can be pointed at an existing note
    When I update note "WikiLinks E2E CI" content using markdown to become:
      """
      Continuous integration relies on [[original text]].
      """
    Then I should see the note content rendered as:
      | Kind           | Text          |
      | dead wiki link | original text |
    When I point dead wiki link "original text" at existing note "WikiLinks E2E Tech"
    Then I should see the note content rendered as:
      | Kind           | Text          |
      | live wiki link | original text |
    When I view the note content as markdown
    Then the note content markdown source should contain "[[WikiLinks E2E Tech|original text]]"

  @mockBrowserTime
  Scenario: Insert a wiki link to a note in the same notebook
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E Tech" note
    And I insert a wiki link to "WikiLinks E2E CI"
    Then I should see the note content rendered as:
      | Kind      | Text             |
      | wiki link | WikiLinks E2E CI |
    And the wiki link "WikiLinks E2E CI" should link to the note with the same title

  Scenario: Renaming a referenced note while keeping visible reference text
    When I update note "WikiLinks E2E Tech" content using markdown to become:
      """
      See [[WikiLinks E2E CI]] for process.
      """
    And I route to the note "WikiLinks E2E CI"
    And I set the note title to "WikiLinks E2E CI Renamed" keeping visible reference text
    And I route to the note "WikiLinks E2E Tech"
    Then I should see the note content rendered as:
      | Kind      | Text             |
      | wiki link | WikiLinks E2E CI |
    And the wiki link "WikiLinks E2E CI" should open the note titled "WikiLinks E2E CI Renamed"

  Scenario: Renaming a referenced note while updating visible reference text
    When I update note "WikiLinks E2E Tech" content using markdown to become:
      """
      See [[WikiLinks E2E CI]] for process.
      """
    And I route to the note "WikiLinks E2E CI"
    And I set the note title to "WikiLinks E2E CI Renamed" updating visible reference text
    And I route to the note "WikiLinks E2E Tech"
    Then I should see the note content rendered as:
      | Kind      | Text                     |
      | wiki link | WikiLinks E2E CI Renamed |
    And the wiki link "WikiLinks E2E CI Renamed" should open the note titled "WikiLinks E2E CI Renamed"

  Scenario: Renaming a referenced note keeps folder-path wiki prefix
    Given I have a notebook "WikiPath Rename NB" with notes:
      | Title                   | Folder                  |
      | WikiPath Rename Old     | WikiPath Rename Recipes |
      | WikiPath Rename Carrier | WikiPath Rename Root    |
    When I update note "WikiPath Rename Carrier" content using markdown to become:
      """
      See [[WikiPath Rename Recipes/WikiPath Rename Old]].
      """
    And I route to the note "WikiPath Rename Old"
    And I set the note title to "WikiPath Rename New" updating visible reference text
    And I route to the note "WikiPath Rename Carrier"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiPath Rename Recipes/WikiPath Rename New]]"
    When I view the note content as rich content
    Then the wiki link "WikiPath Rename Recipes/WikiPath Rename New" should open the note titled "WikiPath Rename New"

  Scenario: Renaming a referenced note rewrites path Markdown hrefs and keeps Markdown
    Given I have a notebook "WikiPathMdRenameNB" with notes:
      | Title                   | Folder                |
      | WikiPathMdRenameOld     | WikiPathMdRenameFolder |
      | WikiPathMdRenameCarrier | WikiPathMdRenameRoot   |
    When I update note "WikiPathMdRenameCarrier" content using markdown to become:
      """
      See [label](/WikiPathMdRenameFolder/WikiPathMdRenameOld.md) and [also](/WikiPathMdRenameFolder/WikiPathMdRenameOld).
      """
    And I route to the note "WikiPathMdRenameOld"
    And I set the note title to "WikiPathMdRenameNew" updating visible reference text
    And I route to the note "WikiPathMdRenameCarrier"
    And I view the note content as markdown
    Then the note content markdown source should contain "[label](/WikiPathMdRenameFolder/WikiPathMdRenameNew.md)"
    And the note content markdown source should contain "[also](/WikiPathMdRenameFolder/WikiPathMdRenameNew)"
    And the note content markdown source should not contain "[["
    When I view the note content as rich content
    Then following the wiki link "label" should open the note titled "WikiPathMdRenameNew"

  @mockBrowserTime
  Scenario: Insert a qualified wiki link to a note in another notebook
    Given I have a notebook "WikiCross Tgt NB" with notes:
      | Title          | Folder             |
      | WikiCross Deep | WikiCross Tgt Root |
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E Tech" note
    And I insert a wiki link to "WikiCross Deep"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiCross Tgt NB:WikiCross Deep]]"
