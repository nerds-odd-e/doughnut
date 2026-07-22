Feature: Notebook workspace index
  As a learner, I want the notebook page to open on the index
  so that the index is primary and admin settings stay off first paint.

  Background:
    Given I am logged in as an existing user

  Scenario: Opening a notebook shows index landmarks without settings sections
    Given I have a notebook "Workspace index suite"
    When I open the notebook "Workspace index suite" from my notebooks catalog
    Then the notebook workspace index shows name "Workspace index suite" and index
    And notebook admin settings sections are not visible

  Scenario: Index edits autosave without opening Settings
    Given I have a notebook "Index edit suite"
    When I open the notebook "Index edit suite" from my notebooks catalog
    Then the notebook workspace index shows name "Index edit suite" and index
    And notebook admin settings sections are not visible
    When I type notebook index body "Index autosave body" on the notebook page and save
    And I reload the notebook page
    Then the notebook workspace index shows name "Index edit suite" and index
    And the notebook index body includes "Index autosave body"
    And notebook admin settings sections are not visible

  Scenario: Settings tab reveals notebook admin sections
    Given I have a notebook "Workspace settings suite"
    When I open the notebook "Workspace settings suite" from my notebooks catalog
    And I open the notebook workspace Settings tab
    Then notebook admin settings sections are visible
