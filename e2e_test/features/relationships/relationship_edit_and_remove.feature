Feature: relationship edit and remove

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

  Scenario: change relation type keeps user-authored content suffix
    When I change the relationship from "Moon" to "Earth" to "a specialization of"
    When I open the note content markdown editor
    Then the note content markdown source should contain "relation: a-specialization-of"
    And the note content markdown source should contain "Observations from orbit."
    And the note content markdown source should not contain "relation: a-part-of"

  Scenario: delete relationship
    When I delete the relationship from "Moon" to "Earth"
    Then I should see "Moon" has no relationship to "Earth"

  Scenario: reduce relationship to source property on delete
    When I delete the relationship from "Moon" to "Earth" and reduce it to a property of the source
    When I open the note content markdown editor on note "Moon"
    Then the note content markdown source should contain "a part of: '[[Earth]]'"
    And I should see "Moon" has no relationship to "Earth"

  Scenario: reduce to source property uses suffixed key when source already has that property
    Given note "Moon" has content:
      """
      ---
      a part of: "[[Mars]]"
      ---

      """
    When I delete the relationship from "Moon" to "Earth" and reduce it to a property of the source
    When I open the note content markdown editor on note "Moon"
    Then the note content markdown source should contain "a part of 2: '[[Earth]]'"
    And I should see "Moon" has no relationship to "Earth"

  Scenario: tracked relationship reduced keeps property memory tracker on source
    Given the note "Moon a part of Earth" was assimilated on day 1
    When I delete the relationship from "Moon" to "Earth" and reduce it to a property of the source
    When I open the note content markdown editor on note "Moon"
    Then the note content markdown source should contain "a part of: '[[Earth]]'"
    And I should see "Moon" has no relationship to "Earth"
    When I am assimilating the note "Moon"
    Then I should see a property memory tracker for "a part of" on the assimilation settings panel
