Feature: Pending wiki links in notes
  As a learner, I want an unconfirmed wiki link to look pending and not act
  like a missing note until save confirms it.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "WikiLinks E2E NB" with notes:
      | Title              | Folder             |
      | WikiLinks E2E Tech | WikiLinks E2E Root |
      | WikiLinks E2E CI   | WikiLinks E2E Root |

  Scenario: An unconfirmed wiki link stays pending until save confirms it is missing
    When I update note "WikiLinks E2E CI" content using markdown to become:
      """
      Saved [[WikiLinks E2E Already Missing]].
      """
    Then I should see the note content rendered as:
      | Kind           | Text                          |
      | dead wiki link | WikiLinks E2E Already Missing |
    When I hold the next note content save
    And I add the wiki link "[[WikiLinks E2E Nowhere]]" in the note content
    Then I should see the note content rendered as:
      | Kind              | Text                          |
      | dead wiki link    | WikiLinks E2E Already Missing |
      | pending wiki link | WikiLinks E2E Nowhere         |
    When I release the held note content save
    Then I should see the note content rendered as:
      | Kind           | Text                          |
      | dead wiki link | WikiLinks E2E Already Missing |
      | dead wiki link | WikiLinks E2E Nowhere         |

  Scenario: An unconfirmed wiki link to an existing note becomes live after save
    When I update note "WikiLinks E2E Tech" content using markdown to become:
      """
      Saved.
      """
    When I hold the next note content save
    And I add the wiki link "[[WikiLinks E2E CI]]" in the note content
    Then I should see the note content rendered as:
      | Kind              | Text             |
      | pending wiki link | WikiLinks E2E CI |
    When I release the held note content save
    Then I should see the note content rendered as:
      | Kind           | Text             |
      | live wiki link | WikiLinks E2E CI |

  Scenario: Following a pending wiki link does not offer creating or pointing at a note
    When I update note "WikiLinks E2E CI" content using markdown to become:
      """
      Saved.
      """
    When I hold the next note content save
    And I add the wiki link "[[WikiLinks E2E Nowhere]]" in the note content
    Then I should see the note content rendered as:
      | Kind              | Text                  |
      | pending wiki link | WikiLinks E2E Nowhere |
    When I follow the pending wiki link "WikiLinks E2E Nowhere"
    Then I should not be offered to create a note or point at an existing note
    When I release the held note content save
    Then I should see the note content rendered as:
      | Kind           | Text                  |
      | dead wiki link | WikiLinks E2E Nowhere |
    When I follow the dead wiki link "WikiLinks E2E Nowhere"
    Then I should be offered to create a note or point at an existing note
