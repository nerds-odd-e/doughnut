Feature: Notebook workspace home
  As a learner, I want the notebook page to open as a workspace home
  so that the index is primary and admin settings stay off first paint.

  Background:
    Given I am logged in as an existing user

  Scenario: Opening a notebook shows home landmarks without settings sections
    Given I have a notebook "Workspace home suite"
    When I open the notebook "Workspace home suite" from my notebooks catalog
    Then the notebook workspace home shows name "Workspace home suite" and index
    And notebook admin settings sections are not visible

  Scenario: Index edits on Home autosave without opening Settings
    Given I have a notebook "Home index edit suite"
    When I open the notebook "Home index edit suite" from my notebooks catalog
    Then the notebook workspace home shows name "Home index edit suite" and index
    And notebook admin settings sections are not visible
    When I type notebook index body "Home index autosave body" on the notebook page and save
    And I reload the notebook page
    Then the notebook workspace home shows name "Home index edit suite" and index
    And the notebook index body includes "Home index autosave body"
    And notebook admin settings sections are not visible

  Scenario: Settings tab reveals notebook admin sections
    Given I have a notebook "Workspace settings suite"
    When I open the notebook "Workspace settings suite" from my notebooks catalog
    And I open the notebook workspace Settings tab
    Then notebook admin settings sections are visible
