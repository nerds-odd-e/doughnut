@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web note moves
  As a notebook owner, I want to pull note moves I made on the web into my local Git checkout so I
  can continue editing at the new path and publish back into the same note.

  Background:
    Given the backend is serving the CLI and install script
    And I install the CLI from localhost without affecting my system
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
    And I assimilate the note "Cells"
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
    And I open the original note route
    And the note content on the current page should be "Membranes and walls"
