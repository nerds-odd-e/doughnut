Feature: Notebook catalog navigation
  As a learner, I want to open a notebook from my catalog
  so that I land on the notebook page with the notebook title and settings.

  Background:
    Given I am logged in as an existing user

  Scenario: Opening a notebook from the catalog lands on the notebook page
    Given I have a notebook with the head note "Notebook Catalog Nav E2E" and details "Head body baseline for catalog open."
    When I navigate to "Notebook Catalog Nav E2E" note
    Then I should be on a notebook page URL without the legacy edit segment
    And the notebook page summary shows title "Notebook Catalog Nav E2E"
