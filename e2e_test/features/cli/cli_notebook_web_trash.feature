@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web trash
  As a notebook owner, I want to pull a web trash and its ordinary Move recovery into my local Git
  checkout so the accepted tree, note identity, and content stay consistent.

  Background:
    Given the backend is serving the CLI and install script
    And I install the CLI from localhost without affecting my system
    And I am logged in as an existing user
    And I have a valid Donut Access Token with label "E2E CLI Clone Token"

  @mockBrowserTime
  Scenario: Pulling a web trash then recovering the note by moving it back to its active folder
    Given I have a notebook "CLI Web Trash Notebook"
    And the notebook "CLI Web Trash Notebook" has an empty folder "Biology"
    And I have a note "Cells" under notebook "CLI Web Trash Notebook" in folder "Biology" with content:
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
    And the notebook "CLI Web Trash Notebook"'s Git binding reflects its current content
    And I capture the note id of "Cells"
    When I clone the notebook "CLI Web Trash Notebook" into a temporary destination using the installed CLI
    And I trash note "Cells"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | Biology/.keep           |
      | _trash/Biology/Cells.md |
    And the cloned checkout file "_trash/Biology/Cells.md" is:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    When I jump to the notebook "CLI Web Trash Notebook"
    And I reload the notebook page
    And I expand the children of note "_trash" in the sidebar
    And I open the folder page for "Biology" under open parent "_trash"
    And I open the note "Cells" from the sidebar
    Then I should see the current note is in trash
    And the note content should include "Membranes"
    When I move the current note under folder "Biology" in notebook "CLI Web Trash Notebook"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | Biology/Cells.md     |
      | _trash/Biology/.keep |
    And the cloned checkout file "Biology/Cells.md" is:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    When I open the original note route
    Then I should see the current note is not in trash
    And the note content should include "Membranes"
