@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web trash
  As a notebook owner, I want to pull a web trash into my local Git checkout, recover the same note
  by an ordinary local move and publication, and keep the accepted tree, note identity, and content
  consistent.

  Background:
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
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
    And the notebook "CLI Web Trash Notebook" uses legacy raw Git attachment storage
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

  @mockBrowserTime
  Scenario: Publishing a local recovery after pulling a web trash
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
    And the notebook "CLI Web Trash Notebook" uses legacy raw Git attachment storage
    And the notebook "CLI Web Trash Notebook"'s Git binding reflects its current content
    And I capture the note id of "Cells"
    When I clone the notebook "CLI Web Trash Notebook" into a temporary destination using the installed CLI
    And I trash note "Cells"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | Biology/.keep           |
      | _trash/Biology/Cells.md |
    When I commit a rename of "_trash/Biology/Cells.md" to "Biology/Cells.md" and a removal of "Biology/.keep" together in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I open the original note route
    Then I should see the current note is not in trash
    And the note content should include "Membranes"
