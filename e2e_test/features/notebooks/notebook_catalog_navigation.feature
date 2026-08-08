Feature: Notebook catalog navigation
  As a learner, I want to open a notebook from my catalog
  so that I can rename it from the notebook page.

  Background:
    Given I am logged in as an existing user

  Scenario: Renaming a notebook opened from the catalog
    Given I have a notebook "Rename me suite"
    And I open the notebook "Rename me suite" from my notebooks catalog
    When I rename the notebook to "Renamed catalog suite"
    And I reload the notebook page
    Then the notebook page summary shows name "Renamed catalog suite"
