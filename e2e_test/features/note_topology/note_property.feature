Feature: Note property location
  As a learner, I want a property to have a web location
  so that I can open that property on the note page.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Property location"
    And I have a note "Vitamins" under notebook "Property location" with content:
      """
      ---
      topic: micronutrients
      wikidata_id: Q34932
      ---

      Vitamin notes body.
      """

  Scenario: Visiting a property location focuses the editable property
    Given I visit note "Vitamins"
    When I visit property "topic" of note "Vitamins"
    Then I should see the note tree in the sidebar
      | note-title |
      | Vitamins   |
    And the rich note property "topic" should be focused with its value dialog open

  Scenario: Visiting a specialized property location focuses the property without a value dialog
    Given I visit note "Vitamins"
    When I visit property "wikidata_id" of note "Vitamins"
    Then I should see the note tree in the sidebar
      | note-title |
      | Vitamins   |
    And the rich note property "wikidata_id" should be focused showing "Q34932" without a value dialog

  Scenario: Visiting a read-only property location focuses the property without a value dialog
    Given I am re-logged in as "another_old_learner"
    And I have a notebook "Shared property location"
    And I have a note "Minerals" under notebook "Shared property location" with content:
      """
      ---
      topic: micronutrients
      ---

      Mineral notes body.
      """
    And notebook "Shared property location" is shared to the Bazaar
    And I am re-logged in as "old_learner"
    And I have subscribed to notebook "Shared property location" in the bazaar with daily assimilation target of 1
    Given I visit note "Minerals"
    When I visit property "topic" of note "Minerals"
    Then I should see the note tree in the sidebar
      | note-title |
      | Minerals   |
    And the rich note property "topic" should be focused showing "micronutrients" without a value dialog
