Feature: link edit and remove

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Space" and notes:
      | Title | Parent Title |
      | Moon  | Space        |
      | Earth | Space        |
      | Mars  | Space        |
    And there is "a part of" link between note "Moon" and "Earth"

  Scenario: change relation type
    When I change the link from "Moon" to "Earth" to "a specialization of"
    And I should see "Moon" has link "a specialization of" "Earth"

  Scenario: change relation type of a reverse link
    When I change the reference from "Earth" to "Moon" to "a specialization of"
    And I should see "Moon" has link "a specialization of" "Earth"

  Scenario: delete link
    When I delete the link from "Moon" to "Earth"
    Then I should see "Moon" has no link to "Earth"
