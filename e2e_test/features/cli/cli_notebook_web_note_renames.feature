@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web note renames
  As a notebook owner, I want to pull note renames I made on the web into my local Git checkout so I
  can continue with ordinary local tools.

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
    And the notebook "CLI Clone Notebook" has a readme-only folder "Recipes" with readme "Folder landing"
    And I have a note "Pasta" under notebook "CLI Clone Notebook" in folder "Recipes" with content:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Boil water
      """
    And the notebook "CLI Clone Notebook" has a readme-only folder "Kitchen" with readme "Kitchen landing"
    And the notebook "CLI Clone Notebook"'s Git binding reflects its current content
    And I have a valid Donut Access Token with label "E2E CLI Clone Token"

  Scenario: Pulling a web note rename that keeps the reference's visible text into a clean checkout
    Given I have a note "Cells" under notebook "CLI Clone Notebook" in folder "Biology" with content:
      """
      ---
      type: Note
      ---
      Cells
      """
    And I have a note "Cell guide" under notebook "CLI Clone Notebook" with content:
      """
      ---
      type: Note
      related: "[[Cells]]"
      ---
      See [[Cells]].
      """
    And the notebook "CLI Clone Notebook"'s Git binding reflects its current content
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I route to the note "Cells"
    And I set the note title to "Cell structure" keeping visible reference text
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | README.md                   |
      | Overview.md                 |
      | Cell guide.md               |
      | Biology/Cell structure.md   |
      | Kitchen/README.md           |
      | Recipes/README.md           |
      | Recipes/Pasta.md            |
    And the cloned checkout file "Biology/Cell structure.md" is:
      """
      ---
      type: Note
      ---
      Cells
      """
    And the cloned checkout file "Cell guide.md" is:
      """
      ---
      type: Note
      related: "[[Cell structure|Cells]]"
      ---
      See [[Cell structure|Cells]].
      """
