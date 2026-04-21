Feature: Wiki Link in Note Details
  As a learner, I want to write wiki links using [[double bracket]] syntax
  in markdown mode and see them as clickable links in rich mode,
  so that I can navigate between related notes easily.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "My Notebook" and notes:
      | Title   | Details       | Parent Title |
      | Hoge    | Hoge details  | My Notebook  |

  @ignore
  Scenario: Single open bracket is not a wiki link
    When I update note "Hoge" details using markdown to become:
      """
      This is just a [ bracket
      """
    Then I should see the rich content of the note with details:
      | Tag | Content                  |
      | p   | This is just a [ bracket |
    And there should be no wiki link in the note details

  @ignore
  Scenario: Empty double brackets are not a wiki link
    When I update note "Hoge" details using markdown to become:
      """
      This has empty [[]] brackets
      """
    Then I should see the rich content of the note with details:
      | Tag | Content                      |
      | p   | This has empty [[]] brackets |
    And there should be no wiki link in the note details

  @ignore
  Scenario: Wiki link to an existing note is shown as a clickable blue link
    When I update note "Hoge" details using markdown to become:
      """
      See also [[Hoge]]
      """
    Then I should see a wiki link "Hoge" in the note details
    And the wiki link "Hoge" should be blue
    When I click the wiki link "Hoge"
    Then I should see "Hoge" in the page

  @ignore
  Scenario: Wiki link to a non-existing note is shown in red without a link
    When I update note "Hoge" details using markdown to become:
      """
      See also [[Fuga]]
      """
    Then I should see a wiki link "Fuga" in the note details
    And the wiki link "Fuga" should be red
    And the wiki link "Fuga" should not be clickable

  @ignore
  Scenario: Wiki link in markdown mode is shown as raw syntax
    When I update note "Hoge" details using markdown to become:
      """
      Link to [[Hoge]] here
      """
    And I switch to edit as markdown mode
    Then the markdown textarea should contain "Link to [[Hoge]] here"

  @ignore
  Scenario: Wiki link to a note with a dot in the title
    Given there are some notes:
      | Title      | Parent Title |
      | Hoge.Fuga  | My Notebook  |
    When I update note "Hoge" details using markdown to become:
      """
      See [[Hoge.Fuga]]
      """
    Then I should see a wiki link "Hoge.Fuga" in the note details
    And the wiki link "Hoge.Fuga" should be blue
    When I click the wiki link "Hoge.Fuga"
    Then I should see "Hoge.Fuga" in the page
