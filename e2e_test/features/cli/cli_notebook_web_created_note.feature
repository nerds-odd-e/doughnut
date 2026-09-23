@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web note changes
  As a notebook owner, I want to pull note changes I made on the web into my local Git checkout so I
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
    And the notebook "CLI Clone Notebook" uses legacy raw Git attachment storage
    And the notebook "CLI Clone Notebook"'s Git binding reflects its current content
    And I have a valid Donut Access Token with label "E2E CLI Clone Token"

  Scenario: Pulling a title-only web-created root note into a clean checkout
    When I keep the CLI notebook across the other worktree's fixture reset
    And I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I create a title-only root note titled "Shopping list" in the notebook "CLI Clone Notebook"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout contains exactly:
      | README.md          |
      | Overview.md        |
      | Shopping list.md   |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
    And the cloned checkout file "Shopping list.md" is:
      """
      ---
      type: Note
      ---

      """

  Scenario: Publishing an edit to a received folder note retains an empty sibling folder
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I create a folder named "Biology" while viewing note "Overview"
    And I create a folder named "Chemistry" while viewing note "Overview"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | README.md          |
      | Overview.md        |
      | Biology/.keep      |
      | Chemistry/.keep    |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
    When I create a note with title "Cells" under the folder "Biology" in the notebook "CLI Clone Notebook"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | README.md          |
      | Overview.md        |
      | Biology/Cells.md   |
      | Chemistry/.keep    |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
    And the cloned checkout file "Biology/Cells.md" is:
      """
      ---
      type: Note
      ---

      """
    When I commit the following edit to "Biology/Cells.md" in the cloned checkout:
      """
      ---
      type: Note
      ---
      Cells have membranes
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Biology/Cells" has content "Cells have membranes"
    And the cloned checkout file "Chemistry/.keep" is unchanged from its parent

  Scenario: Pulling nested web authoring with one pull
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I create a folder named "Science" while viewing note "Overview"
    And I create a folder named "Biology" under folder "Science" in notebook "CLI Clone Notebook"
    And I create a note with title "Cells" under the folder "Biology" in the notebook "CLI Clone Notebook"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | README.md                   |
      | Overview.md                 |
      | Science/Biology/Cells.md    |
      | Kitchen/README.md           |
      | Recipes/README.md           |
      | Recipes/Pasta.md            |

  @publicationProfile
  Scenario: Measuring a small publication on an owned disposable backend
    When I seed the representative publication baseline
    And I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I prepare the small deterministic publication profile
    And I start recording the owned publication JVM
    And I publish and time the profiling proposal
    And I stop recording the owned publication JVM
    Then the installed CLI reports the committed change as the accepted head
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout is a clean checkout of the accepted head
    And the received checkout contains every profiling document unchanged

  @publicationProfileRejection
  Scenario: Measuring late publication rejection on an owned disposable backend
    When I seed the representative publication baseline
    And I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I prepare the small deterministic publication profile
    And I invalidate the last processed profiling addition
    And I start recording the owned publication JVM
    And I publish and time the rejected profiling proposal
    And I stop recording the owned publication JVM
    When I pull the second cloned checkout using the installed CLI
    Then the profiling baseline and learning state are preserved

  @publicationProfileHttp
  Scenario: Measuring HTTP publication on an owned disposable backend
    When I seed the representative publication baseline
    And I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I prepare the small deterministic publication profile
    And I start recording the owned publication JVM
    And I publish and time the profiling proposal through benchmark HTTP
    And I stop recording the owned publication JVM
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout is a clean checkout of the accepted head
    And the received checkout contains every profiling document unchanged

  @publicationProfileHttpRejection
  Scenario: Measuring late HTTP publication rejection on an owned disposable backend
    When I seed the representative publication baseline
    And I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I prepare the small deterministic publication profile
    And I invalidate the last processed profiling addition
    And I start recording the owned publication JVM
    And I publish and time the rejected profiling proposal through benchmark HTTP
    And I stop recording the owned publication JVM
    When I pull the second cloned checkout using the installed CLI
    Then the profiling baseline and learning state are preserved

  @publicationProfileHttpUpdate
  Scenario: Measuring HTTP publication of existing-note edits on an owned disposable backend
    When I seed the representative publication baseline
    And I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I prepare the small deterministic existing-note-edit publication profile
    And I start recording the owned publication JVM
    And I publish and time the profiling proposal through benchmark HTTP
    And I stop recording the owned publication JVM
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout is a clean checkout of the accepted head
    And the accepted edit yields exact updated aliases and property-reference targets
    And the received checkout contains every profiling document unchanged
