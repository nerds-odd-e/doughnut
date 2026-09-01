Feature: Insert wiki links in notes
  As a learner, I want to insert wiki-style links while editing so the stored
  Portable path identifies the chosen destination uniquely.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "WikiLinks E2E NB" with notes:
      | Title              | Folder             |
      | WikiLinks E2E Tech | WikiLinks E2E Root |
      | WikiLinks E2E CI   | WikiLinks E2E Root |

  @mockBrowserTime
  Scenario: Insert a wiki link to a note in the same notebook
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E Tech" note
    And I insert a wiki link to "WikiLinks E2E CI"
    Then I should see the note content rendered as:
      | Kind      | Text             |
      | wiki link | WikiLinks E2E CI |
    And the wiki link "WikiLinks E2E CI" should link to the note with the same title
    When I view the note content as markdown
    Then the note content markdown source should contain "[[WikiLinks E2E CI]]"

  @mockBrowserTime
  Scenario: Insert a wiki link to a colliding display name uses the full Portable path
    Given I have a notebook "WikiDup Insert NB" with notes:
      | Title           | Folder          |
      | WikiDup Shared  | WikiDup Recipes |
      | WikiDup Carrier | WikiDup Root    |
    And I have a notebook "WikiDup Insert NB" with notes:
      | Title          | Folder         |
      | WikiDup Shared | WikiDup Pantry |
    When I navigate to "WikiDup Insert NB/WikiDup Root/WikiDup Carrier" note
    And I insert a wiki link to "WikiDup Shared" in folder "WikiDup Recipes"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiDup Recipes/WikiDup Shared]]"

  @mockBrowserTime
  Scenario: Insert a qualified wiki link to a note in another notebook
    Given I have a notebook "WikiCross Tgt NB" with notes:
      | Title          | Folder             |
      | WikiCross Deep | WikiCross Tgt Root |
    When I navigate to "WikiLinks E2E NB/WikiLinks E2E Root/WikiLinks E2E Tech" note
    And I insert a wiki link to "WikiCross Deep"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiCross Tgt NB:WikiCross Deep]]"
