Feature: relationship edit and remove

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Space topics" with a note "Space" and notes:
      | Title | Parent Title |
      | Moon  | Space        |
      | Earth | Space        |
      | Mars  | Space        |
    And there is "a part of" relationship between note "Moon" and "Earth"

  Scenario: change relation type
    When I change the relationship from "Moon" to "Earth" to "a specialization of"
    And I should see "Moon" has relationship "a specialization of" "Earth"
    When I open the relationship from "Moon" to "Earth"
    Then I should see the relationship note title reflects "Moon" "a specialization of" "Earth"
    When I open the note details markdown editor
    Then the note details markdown source should contain "relation: a-specialization-of"
    And the note details markdown source should contain "[[Moon]] a specialization of [[Earth]]"
    And the note details markdown source should not contain "relation: a-part-of"
    And the note details markdown source should not contain "[[Moon]] a part of [[Earth]]"

  Scenario: change relation type of a reverse relationship
    When I change the reference from "Earth" to "Moon" to "a specialization of"
    And I should see "Moon" has relationship "a specialization of" "Earth"
    When I open the relationship from "Moon" to "Earth"
    Then I should see the relationship note title reflects "Moon" "a specialization of" "Earth"
    When I open the note details markdown editor
    Then the note details markdown source should contain "relation: a-specialization-of"
    And the note details markdown source should contain "[[Moon]] a specialization of [[Earth]]"
    And the note details markdown source should not contain "relation: a-part-of"
    And the note details markdown source should not contain "[[Moon]] a part of [[Earth]]"

  Scenario: change relation type keeps user-authored details suffix
    When I open the relationship from "Moon" to "Earth"
    And I update the current note details using markdown to become:
      """
      ---
      type: relationship
      relation: a-part-of
      source: "[[Moon]]"
      target: "[[Earth]]"
      ---

      [[Moon]] a part of [[Earth]].

      Observations from orbit.
      """
    And I flush pending note details save
    When I change the relationship from "Moon" to "Earth" to "a specialization of"
    Then I should see the relationship note title reflects "Moon" "a specialization of" "Earth"
    When I open the note details markdown editor
    Then the note details markdown source should contain "relation: a-specialization-of"
    And the note details markdown source should contain "Observations from orbit."
    And the note details markdown source should not contain "relation: a-part-of"

  Scenario: delete relationship
    When I delete the relationship from "Moon" to "Earth"
    Then I should see "Moon" has no relationship to "Earth"
