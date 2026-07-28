Feature: Notebook export

  Background:
    Given I am logged in as an existing user
    And I have a notebook "E2E Export Notebook" with a note "Export Root Note"

  Scenario: Export notebook downloads a zip
    When I export notebook "E2E Export Notebook" from the catalog
    Then a zip file for notebook "E2E Export Notebook" should be downloaded
