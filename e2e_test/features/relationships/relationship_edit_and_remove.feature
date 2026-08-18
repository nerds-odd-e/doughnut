Feature: Relationship edit and remove
  As a learner, I want to change or remove relationships between notes
  so that my note topology stays accurate.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Space topics" with notes:
      | Title |
      | Space |
      | Moon |
      | Earth |
      | Mars |
    And there is "a part of" relationship between note "Moon" and "Earth" in notebook "Space topics" with body suffix:
      """
      Observations from orbit.
      """

  Scenario: Changing relation type keeps user-authored content suffix
    When I change the relationship from "Moon" to "Earth" to "a specialization of"
    And I open the note content markdown editor
    Then the note content markdown source should contain "relation: a-specialization-of"
    And the note content markdown source should contain "Observations from orbit."
    And the note content markdown source should not contain "relation: a-part-of"

  Scenario: Deleting a relationship
    When I delete the relationship from "Moon" to "Earth"
    Then I should see "Moon" has no relationship to "Earth"

  Scenario: Reducing a relationship to a source property on delete
    When I delete the relationship from "Moon" to "Earth" and reduce it to a property of the source
    And I open the note content markdown editor on note "Moon"
    Then the note content markdown source should contain "a part of: '[[Earth]]'"
    And I should see "Moon" has no relationship to "Earth"

  Scenario: Reducing to source property uses a suffixed key when the property already exists
    Given note "Moon" has content:
      """
      ---
      a part of: "[[Mars]]"
      ---

      """
    When I delete the relationship from "Moon" to "Earth" and reduce it to a property of the source
    And I open the note content markdown editor on note "Moon"
    Then the note content markdown source should contain "a part of 2: '[[Earth]]'"
    And I should see "Moon" has no relationship to "Earth"

  Scenario: Reducing a relationship whose source is path Markdown
    Given I have a notebook "Path reduce space" with notes:
      | Title  | Content    |
      | Phobos | Lunar body |
      | Deimos | Moon       |
    And I have a note "Phobos a part of Deimos" under notebook "Path reduce space" with content:
      """
      ---
      type: Relationship
      relation: a-part-of
      source: "[Phobos](/Phobos.md)"
      target: "[[Deimos]]"
      ---

      """
    When I delete the relationship from "Phobos" to "Deimos" and reduce it to a property of the source
    And I open the note content markdown editor on note "Phobos"
    Then the note content markdown source should contain "a part of: '[[Deimos]]'"
    And I should see "Phobos" has no relationship to "Deimos"

  Scenario: Tracked relationship reduced keeps property memory tracker on source
    Given the note "Moon a part of Earth" was assimilated on day 1
    When I delete the relationship from "Moon" to "Earth" and reduce it to a property of the source
    And I open the note content markdown editor on note "Moon"
    Then the note content markdown source should contain "a part of: '[[Earth]]'"
    And I should see "Moon" has no relationship to "Earth"
    When I am assimilating the note "Moon"
    Then I should see a property memory tracker for "a part of"
