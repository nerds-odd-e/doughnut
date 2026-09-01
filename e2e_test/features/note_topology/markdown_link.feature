Feature: Markdown links in notes
  As a learner, I want Markdown links to keep their authored URLs through
  edit and reopen so the editor never converts them into wiki syntax.

  Background:
    Given I am logged in as an existing user

  Scenario Outline: Authored Markdown URLs round-trip through save and reopen
    Given I have a notebook "MdLinkNB" with notes:
      | Title      |
      | MdLinkNote |
    When I update note "MdLinkNote" content using markdown to become:
      """
      See <markdown>.
      """
    When I reload the current page for note "MdLinkNote"
    And I view the note content as markdown
    Then the note content markdown source should contain "<markdown>"
    And the note content markdown source should not contain "[["

    Examples:
      | markdown                                      |
      | [label](/n1234)                               |
      | [label](https://doughnut.odd-e.com/n19921)    |
      | [shown](/n99/p/topic)                         |

  Scenario Outline: File-looking Markdown URLs keep ordinary link UI
    Given I have a notebook "MdFileLinkNB" with notes:
      | Title          | Folder         |
      | MdFileLinkNote | MdFileLinkFold |
    When I update note "MdFileLinkNote" content using markdown to become:
      """
      See <markdown>.
      """
    Then I should see the note content rendered as:
      | Kind           | Text      |
      | markdown link  | <display> |
    When I view the note content as markdown
    Then the note content markdown source should contain "<markdown>"
    And the note content markdown source should not contain "[["

    Examples:
      | markdown                          | display |
      | [Target](/MdFileLinkFold/Target.md) | Target  |
      | [Target](folder/Target.md)          | Target  |

  Scenario Outline: File-looking Markdown URLs do not create wiki references
    Given I have a notebook "MdNoWikiRefNB" with notes:
      | Title  | Folder      |
      | Target | MdRefFolder |
      | Source |             |
    When I update note "Source" content using markdown to become:
      """
      See <markdown>.
      """
    Then I should see the note content rendered as:
      | Kind          | Text   |
      | markdown link | Target |
    When I navigate to "MdNoWikiRefNB/MdRefFolder/Target" note
    Then I should not see the References section

    Examples:
      | markdown                         |
      | [Target](/MdRefFolder/Target.md) |
      | [Target](MdRefFolder/Target.md)  |

  Scenario: A root-relative Donut note URL contributes a semantic reference
    Given I have a notebook "MdNoteUrlNB" with notes:
      | Title         |
      | MdUrlTarget   |
      | MdUrlSource   |
    When I update note "MdUrlSource" content using markdown to become a note URL to "MdUrlTarget" with display "wrong title"
    Then I should see the note content rendered as:
      | Kind          | Text         |
      | markdown link | wrong title  |
    When I view the note content as markdown
    Then the note content markdown source should contain a note URL to "MdUrlTarget" with display "wrong title"
    And the note content markdown source should not contain "[["
    When I navigate to "MdNoteUrlNB/MdUrlTarget" note
    Then I should see the References section
    And I should see "MdUrlSource" in the References section
