Feature: Notebook catalog navigation
  As a learner, I want to open a notebook from my catalog
  so that I land on the notebook page with the notebook name and settings.

  Background:
    Given I am logged in as an existing user

  Scenario: Opening a notebook from the catalog lands on the notebook page
    Given I have a notebook "Catalog nav suite" with a note "Notebook Catalog Nav E2E" and details "Head body baseline for catalog open."
    When I open the notebook "Catalog nav suite" from my notebooks catalog
    And the notebook page summary shows name "Catalog nav suite"
