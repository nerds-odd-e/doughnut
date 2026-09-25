@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web note moves
  As a notebook owner, I want to pull note moves I made on the web into my local Git checkout so I
  can continue editing at the new path and publish back into the same note.

  Background:
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And I am logged in as an existing user
    And I have a notebook "CLI Clone Notebook"
    And I have a note "Overview" under notebook "CLI Clone Notebook" with content:
      """
      ---
      type: Note
      ---
      """
    And the notebook "CLI Clone Notebook" has readme content "Notebook landing"
    And the notebook "CLI Clone Notebook" has a readme-only folder "Biology" with readme "Biology landing"
    And the notebook "CLI Clone Notebook" has a readme-only folder "Study" with readme "Study landing"
    And the notebook "CLI Clone Notebook"'s Git binding reflects its current content
    And I have a valid Donut Access Token with label "E2E CLI Clone Token"

  @mockBrowserTime
  Scenario: Pulling a web note move and continuing local editing at the new path
    Given I have a note "Cells" under notebook "CLI Clone Notebook" in folder "Biology" with content:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    And I assimilated one note "Cells" at the current time
    And the notebook "CLI Clone Notebook"'s Git binding reflects its current content
    And I capture the note id of "Cells"
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I route to the note "Cells"
    And I move the current note under folder "Study" in notebook "CLI Clone Notebook"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | README.md         |
      | Overview.md       |
      | Biology/README.md |
      | Study/README.md   |
      | Study/Cells.md    |
    And the cloned checkout file "Study/Cells.md" is:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    When I commit the following edit to "Study/Cells.md" in the cloned checkout:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes and walls
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the original note in Donut should be "CLI Clone Notebook/Study/Cells" with content:
      """
      Cells
      =====

      Membranes and walls
      """

  @mockBrowserTime
  Scenario: Pulling a linked note move receives the rewritten in-notebook reference
    Given I have a note "Cells" under notebook "CLI Clone Notebook" in folder "Biology" with content:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    And I have a note "Reading" under notebook "CLI Clone Notebook" with content:
      """
      ---
      type: Note
      related: "[[Biology/Cells|shown]]"
      ---
      See [[Biology/Cells|shown]] for details.
      """
    And the notebook "CLI Clone Notebook"'s Git binding reflects its current content
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I route to the note "Cells"
    And I move the current note under folder "Study" in notebook "CLI Clone Notebook"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | README.md         |
      | Overview.md       |
      | Reading.md        |
      | Biology/README.md |
      | Study/README.md   |
      | Study/Cells.md    |
    And the cloned checkout file "Reading.md" is:
      """
      ---
      type: Note
      related: '[[Study/Cells|shown]]'
      ---
      See [[Study/Cells|shown]] for details.
      """

  @mockBrowserTime
  Scenario: Pulling a web note move preserves empty-folder markers when the moved note changes which folders are empty
    Given I have a notebook "CLI Empty Folder Move Notebook"
    And the notebook "CLI Empty Folder Move Notebook" has an empty folder "Biology"
    And the notebook "CLI Empty Folder Move Notebook" has an empty folder "Study"
    And I have a note "Cells" under notebook "CLI Empty Folder Move Notebook" in folder "Biology" with content:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    And I assimilated one note "Cells" at the current time
    And the notebook "CLI Empty Folder Move Notebook"'s Git binding reflects its current content
    And I capture the note id of "Cells"
    When I clone the notebook "CLI Empty Folder Move Notebook" into a temporary destination using the installed CLI
    And I route to the note "Cells"
    And I move the current note under folder "Study" in notebook "CLI Empty Folder Move Notebook"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | Biology/.keep |
      | Study/Cells.md |
    When I commit the following edit to "Study/Cells.md" in the cloned checkout:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes and walls
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the original note in Donut should be "CLI Empty Folder Move Notebook/Study/Cells" with content:
      """
      Cells
      =====

      Membranes and walls
      """
