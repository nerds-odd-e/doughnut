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
