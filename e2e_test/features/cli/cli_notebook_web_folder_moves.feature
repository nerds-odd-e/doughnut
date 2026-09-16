@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web folder moves
  As a notebook owner, I want to pull a folder move I made on the web into my local Git checkout so
  the folder exists exactly once at its new path.

  Background:
    Given the backend is serving the CLI and install script
    And I install the CLI from localhost without affecting my system
    And I am logged in as an existing user
    And I have a notebook "CLI Web Folder Move Notebook"
    And the notebook "CLI Web Folder Move Notebook" has an empty folder "Study"
    And I have a note "Cells" under notebook "CLI Web Folder Move Notebook" in folder "Biology" with content:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    And I assimilate the note "Cells"
    And the notebook "CLI Web Folder Move Notebook"'s Git binding reflects its current content
    And I have a valid Donut Access Token with label "E2E CLI Clone Token"

  @mockBrowserTime
  Scenario: Pulling a web folder move places the folder exactly once at the new path
    When I clone the notebook "CLI Web Folder Move Notebook" into a temporary destination using the installed CLI
    And I open the folder page for "Biology" in notebook "CLI Web Folder Move Notebook"
    And I move the current folder to notebook "CLI Web Folder Move Notebook" folder "Study"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | Study/Biology/Cells.md |
    And the cloned checkout file "Study/Biology/Cells.md" is:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
