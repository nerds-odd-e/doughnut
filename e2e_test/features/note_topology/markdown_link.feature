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
