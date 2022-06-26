Feature: link edit and remove

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title | skipReview | testingParent |
      | Space | true       |               |
      | Moon  | true       | Space         |
      | Earth | true       | Space         |
      | Mars  | true       | Space         |
    And there is "a part of" link between note "Moon" and "Earth"

  Scenario: change link type
    When I change the link from "Moon" to "Earth" to "a specialization of"
    And On the current page, I should see "Moon" has link "a specialization of" "Earth"

  Scenario: change link type of a reverse link
    When I change the link from "Earth" to "Moon" to "a specialization of"
    And On the current page, I should see "Earth" has link "a specialization of" "Moon"

  Scenario: delete link
    When I delete the link from "Moon" to "Earth"
    Then I should see "Moon" has no link to "Earth"

  Scenario: delete reverse link
    When I delete the link from "Earth" to "Moon"
    Then I should see "Moon" has no link to "Earth"
