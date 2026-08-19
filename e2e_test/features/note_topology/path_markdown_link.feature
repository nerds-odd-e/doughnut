Feature: Path Markdown links in notes
  As a learner, I want path Markdown links in my note content so they open like
  wiki links, show unresolved targets as dead wiki links, and keep their spelling.

  Background:
    Given I am logged in as an existing user

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

  Scenario: Unresolved path Markdown shows as a dead wiki link
    Given I have a notebook "WikiPathMdDeadNB" with notes:
      | Title                 | Folder               |
      | WikiPathMdDeadCarrier | WikiPathMdDeadFolder |
    When I update note "WikiPathMdDeadCarrier" content using markdown to become:
      """
      See [label](/WikiPathMdDeadFolder/WikiPathMdDeadMissing.md).
      """
    Then I should see the note content rendered as:
      | Kind           | Text  |
      | dead wiki link | label |
    When I view the note content as markdown
    Then the note content markdown source should contain "[label](/WikiPathMdDeadFolder/WikiPathMdDeadMissing.md)"
    And the note content markdown source should not contain "[["

  @mockBrowserTime
  Scenario: Pointing a dead path Markdown link at an existing note keeps Markdown
    Given I have a notebook "WikiPathMdPointNB" with notes:
      | Title                  | Folder                 |
      | WikiPathMdPointTitle   | WikiPathMdChosenFolder |
      | WikiPathMdPointCarrier | WikiPathMdPointRoot    |
    When I update note "WikiPathMdPointCarrier" content using markdown to become:
      """
      See [label](/WikiPathMdDeadFolder/WikiPathMdMissing.md).
      """
    Then I should see the note content rendered as:
      | Kind           | Text  |
      | dead wiki link | label |
    When I point dead wiki link "label" at existing note "WikiPathMdPointTitle"
    Then I should see the note content rendered as:
      | Kind           | Text  |
      | live wiki link | label |
    When I view the note content as markdown
    Then the note content markdown source should contain "[label](/WikiPathMdChosenFolder/WikiPathMdPointTitle.md)"
    And the note content markdown source should not contain "[["
    When I view the note content as rich content
    Then following the wiki link "label" should open the note titled "WikiPathMdPointTitle"

  Scenario: Renaming a referenced note rewrites path Markdown hrefs and keeps Markdown
    Given I have a notebook "WikiPathMdRenameNB" with notes:
      | Title                   | Folder                 |
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

  Scenario: Renaming a folder updates path prefixes in both spellings
    Given I have a notebook "WikiFolderRenameNB" with notes:
      | Title                   | Folder                 |
      | WikiFolderRenameTitle   | WikiFolderRenameOld    |
      | WikiFolderRenameCarrier | WikiFolderRenameRoot   |
    When I update note "WikiFolderRenameCarrier" content using markdown to become:
      """
      See [[WikiFolderRenameOld/WikiFolderRenameTitle]] and [label](/WikiFolderRenameOld/WikiFolderRenameTitle.md).
      """
    And I open the folder page for "WikiFolderRenameOld" in notebook "WikiFolderRenameNB"
    And I rename the folder heading to "WikiFolderRenameNew"
    And I reload the folder page
    Then the folder page heading should be "WikiFolderRenameNew"
    When I route to the note "WikiFolderRenameCarrier"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiFolderRenameNew/WikiFolderRenameTitle]]"
    And the note content markdown source should contain "[label](/WikiFolderRenameNew/WikiFolderRenameTitle.md)"
    When I view the note content as rich content
    Then the wiki link "WikiFolderRenameNew/WikiFolderRenameTitle" should open the note titled "WikiFolderRenameTitle"
    When I route to the note "WikiFolderRenameCarrier"
    Then following the wiki link "label" should open the note titled "WikiFolderRenameTitle"
