@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web note changes
  As a notebook owner, I want to pull note changes I made on the web into my local Git checkout so I
  can continue with ordinary local tools.

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

  Scenario: Pulling a web-created empty folder and then its first note
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I create a folder named "Biology" while viewing note "Overview"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | README.md          |
      | Overview.md        |
      | Biology/.keep      |
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
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
    And the cloned checkout file "Biology/Cells.md" is:
      """
      ---
      type: Note
      ---

      """

  Scenario: Pulling a web note rename into a clean checkout
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
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I update note title "Cells" to become "Cell structure"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout retains its original head as an ancestor and is clean at the accepted head
    And the cloned checkout contains exactly:
      | README.md                   |
      | Overview.md                 |
      | Biology/Cell structure.md   |
      | Kitchen/README.md           |
      | Recipes/README.md           |
      | Recipes/Pasta.md            |
    And the cloned checkout file "Biology/Cell structure.md" is:
      """
      ---
      author: Linnaeus
      type: Note
      ---
      Cells
      =====

      Membranes
      """

  Scenario Outline: Pulling a web note rename and its selected reference rewrite into a clean checkout
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
    And I set the note title to "Cell structure" using <referenceChoice> reference handling
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
      related: '<expectedReference>'
      ---
      See <expectedReference>.
      """

    Examples:
      | referenceChoice    | expectedReference                |
      | KEEP_VISIBLE_TEXT  | [[Cell structure\|Cells]]        |
      | UPDATE_VISIBLE_TEXT | [[Cell structure]]               |

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
