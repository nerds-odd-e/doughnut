Feature: Notebook catalog navigation
  As a learner, I want to open a notebook from my catalog
  so that I land on the notebook entry content (today: the head note).

  Background:
    Given I am logged in as an existing user

  Scenario: Opening a notebook from the catalog shows the head note title and body
    Given I have a notebook with the head note "Notebook Catalog Nav E2E" and details "Head body baseline for catalog open."
    When I navigate to "Notebook Catalog Nav E2E" note
    Then the note title should be "Notebook Catalog Nav E2E"
    And the note details should include "Head body baseline for catalog open."
