Feature: Notebook workspace readme
  As a learner, I want the notebook page to open on the readme
  so that the readme is primary and admin settings stay off first paint.

  Background:
    Given I am logged in as an existing user

  Scenario: Opening a notebook shows readme landmarks without settings sections
    Given I have a notebook "Workspace readme suite"
    When I open the notebook "Workspace readme suite" from my notebooks catalog
    Then the notebook workspace readme shows name "Workspace readme suite" and readme
    And notebook admin settings sections are not visible

  Scenario: Readme edits autosave without opening Settings
    Given I have a notebook "Readme edit suite"
    When I open the notebook "Readme edit suite" from my notebooks catalog
    Then the notebook workspace readme shows name "Readme edit suite" and readme
    And notebook admin settings sections are not visible
    When I type notebook readme body "Readme autosave body" on the notebook page and save
    And I reload the notebook page
    Then the notebook workspace readme shows name "Readme edit suite" and readme
    And the notebook readme body includes "Readme autosave body"
    And notebook admin settings sections are not visible

  Scenario: Settings tab reveals notebook admin sections
    Given I have a notebook "Workspace settings suite"
    When I open the notebook "Workspace settings suite" from my notebooks catalog
    And I open the notebook workspace Settings tab
    Then notebook admin settings sections are visible
