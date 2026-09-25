@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web folder moves
  As a notebook owner, I want to pull a folder move or trash I made on the web into my local Git
  checkout so the folder exists exactly once at its new path.

  Background:
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
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
    And I assimilated one note "Cells" at the current time
    And the notebook "CLI Web Folder Move Notebook"'s Git binding reflects its current content
    And I have a valid Donut Access Token with label "E2E CLI Clone Token"

  @mockBrowserTime
  Scenario: Pulling successive web folder moves receives both accepted heads
    When I clone the notebook "CLI Web Folder Move Notebook" into a temporary destination using the installed CLI
    And I open the folder page for "Biology" in notebook "CLI Web Folder Move Notebook"
    And I move the current folder to notebook "CLI Web Folder Move Notebook" folder "Study"
    And I record the accepted head of notebook "CLI Web Folder Move Notebook"
    And I move the current folder to notebook root
    And I record the accepted head of notebook "CLI Web Folder Move Notebook"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains the recorded accepted heads as ancestors and is clean at the accepted head
    And the cloned checkout contains exactly:
      | Biology/Cells.md |
      | Study/.keep |

  @mockBrowserTime
  Scenario: Pulling a web folder trash then recovering it by ordinary move
    Given I have a notebook "CLI Web Folder Trash Notebook"
    And I have a note "Cells" under notebook "CLI Web Folder Trash Notebook" in folder "Research/Biology" with content:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    And the notebook "CLI Web Folder Trash Notebook" has a folder "Empty" under note "Cells"
    And the notebook "CLI Web Folder Trash Notebook" has its folder "Biology" with readme "Biology landing"
    And the notebook "CLI Web Folder Trash Notebook"'s Git binding reflects its current content
    When I clone the notebook "CLI Web Folder Trash Notebook" into a temporary destination using the installed CLI
    And I open the folder page for "Biology" in notebook "CLI Web Folder Trash Notebook"
    And I trash the current folder
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | Research/.keep |
      | _trash/Research/Biology/README.md |
      | _trash/Research/Biology/Cells.md |
      | _trash/Research/Biology/Empty/.keep |
    And the cloned checkout file "_trash/Research/Biology/Cells.md" is:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """
    When I open the folder page for "Biology" in notebook "CLI Web Folder Trash Notebook"
    And I move the current folder to notebook root
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | Research/.keep |
      | Biology/README.md |
      | Biology/Cells.md |
      | Biology/Empty/.keep |
      | _trash/Research/.keep |
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
