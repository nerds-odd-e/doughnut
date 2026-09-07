@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook clone
  As a learner, I want to clone my notebook to a local Git checkout using the Donut CLI so I
  can read and edit it with ordinary tools.

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

  Scenario: Denied clone explains the permission problem without creating a checkout
    Given the installed CLI uses the access token of "another_old_learner"
    When I clone the notebook "CLI Clone Notebook" expecting rejection from the installed CLI
    Then I should see "does not have permission" in the non-interactive output
    And the clone destination does not exist

  Scenario: Cloning an owned notebook produces a clean canonical Git checkout
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    Then the cloned checkout is a clean single-commit checkout on branch "main"
    And the cloned checkout contains exactly:
      | README.md          |
      | Overview.md        |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
    When I open the notebook "CLI Clone Notebook" from the notebook catalog
    Then the notebook readme body includes "Notebook landing"

  Scenario: Publishing a committed note edit updates the same Donut note
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit the following edit to "Recipes/Pasta.md" in the cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And note "Pasta" should have content "Simmer until al dente"

  Scenario: Publishing a committed note removal deletes the note in Donut
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit a removal of "Recipes/Pasta.md" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I open the notebook "CLI Clone Notebook" from the notebook catalog
    And I open the folder page for "Recipes" from the sidebar
    Then I should see the note tree in the sidebar
      | note-title |
      | Overview   |

  Scenario: Publishing related additions and an edit updates every Donut note
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit the following related additions and edit together in the cloned checkout:
      | path              | content                                                                                   |
      | Shopping.md       | ---\ntype: Note\n---\nBuy fresh basil                                                       |
      | Recipes/Sauce.md  | ---\ntype: Note\n---\nSimmer tomatoes with basil                                            |
      | Recipes/Pasta.md  | ---\ntype: Note\nauthor: Chef Boyardee\n---\nServe al dente pasta with tomato sauce          |
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Shopping" has content "Buy fresh basil"
    And I should see note "CLI Clone Notebook/Recipes/Sauce" has content "Simmer tomatoes with basil"
    And I should see note "CLI Clone Notebook/Recipes/Pasta" has content "Serve al dente pasta with tomato sauce"

  Scenario: Publishing a committed note rename updates the same Donut note under its new title
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit a rename of "Recipes/Pasta.md" to "Recipes/Pasta basics.md" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Recipes/Pasta basics" has content "Boil water"

  Scenario: Publishing a committed note relocation updates the same Donut note at the notebook root
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit a rename of "Recipes/Pasta.md" to "Pasta basics.md" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Pasta basics" has content "Boil water"

  Scenario: Publishing a committed folder relocation updates the same Donut note at the new folder path
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit a rename of "Recipes" to "Kitchen/Recipes" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Kitchen/Recipes/Pasta" has content "Boil water"

  Scenario: Rejecting duplicate metadata keeps the local proposal available for correction
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I add and commit the following note at "Duplicate Keys.md" in the cloned checkout:
      """
      ---
      type: Note
      author: first
      author: second
      ---
      Body.
      """
    And I publish the cloned checkout expecting rejection from the installed CLI
    Then I should see "Duplicate Keys.md" in the non-interactive output
    And I should see "duplicate" in the non-interactive output
    And the cloned checkout retains the original committed proposal
    When I open the notebook "CLI Clone Notebook" from the notebook catalog
    Then I should see the note tree in the sidebar
      | note-title |
      | Overview   |

  Scenario: Publishing a nested-metadata note preserves metadata through a rich body edit
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    Then I should see "one new commit directly on the accepted main containing either one or more added Markdown notes with optional edits, a single edited Markdown note, one isolated equal-content Markdown note remove/add pair that may change folder and/or filename, or one isolated Markdown note deletion that leaves existing links authored; creating a new or unrepresented folder, overwriting an existing note, moving a folder or README, and relocating or renaming together with a content edit in the same commit, are not supported yet. To preserve note identity, commit and publish the unchanged relocation or rename, wait for it to be accepted, then edit and separately commit and publish the content change. Authored referring links are not rewritten by a relocation or rename, so links to the old path may no longer resolve. Do not delete and recreate the note. Use the notebook root or existing folders represented in accepted history." in the non-interactive output
    When I add and commit the following note at "Recipes/Pantry Staples.md" in the cloned checkout:
      """
      ---
      type: Note
      # Author annotation
      custom:
        source: 'local'
      ---
      Keep semolina pasta stocked.
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Recipes/Pantry Staples" has content "Keep semolina pasta stocked."
    When I view the note content as rich content
    And I update note "Pantry Staples" content to become "Restock semolina pasta."
    And I reload the current page for note "Pantry Staples"
    Then the note content should include "Restock semolina pasta."
    When I open the note content markdown editor
    Then the note content markdown source should contain "# Author annotation"
    And the note content markdown source should contain "custom:"
    And the note content markdown source should contain "  source: 'local'"

  Scenario: Pulling then publishing keeps a local Pasta edit and a web Overview save
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit the following edit to "Recipes/Pasta.md" in the cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And I open the note "Overview" for editing
    And I view the note content as rich content
    And I update note "Overview" content to become "Weekly meal plan"
    When I pull the cloned checkout using the installed CLI
    Then the cloned checkout is a clean rebased child of the accepted head
    And the cloned checkout retains the original local commit for "Recipes/Pasta.md"
    And "Overview.md" in the cloned checkout matches the accepted parent
    And the cloned checkout file "Recipes/Pasta.md" is:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And note "Pasta" should have content "Boil water"
    When I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the rebased local head as the accepted head
    And I should see note "CLI Clone Notebook/Recipes/Pasta" has content "Simmer until al dente"
    And I should see note "CLI Clone Notebook/Overview" has content "Weekly meal plan"

  Scenario: Resolving a Pasta conflict then publishing updates Donut with the chosen text
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit the following edit to "Recipes/Pasta.md" in the cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And I open the note "Pasta" for editing
    And I view the note content as rich content
    And I update note "Pasta" content to become "Salt the water first"
    When I pull the cloned checkout expecting rejection from the installed CLI
    Then I should see "Git paused a rebase with a conflict in" in the non-interactive output
    And I should see "Recipes/Pasta.md" in the non-interactive output
    And I should see "git rebase --continue" in the non-interactive output
    And the cloned checkout has a paused rebase conflict for "Recipes/Pasta.md"
    And note "Pasta" should have content "Salt the water first"
    When I write, stage, and continue the cloned checkout rebase with the following edit to "Recipes/Pasta.md":
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Finish in the sauce
      """
    Then the cloned checkout is a clean resolved child of the accepted head
    And the cloned checkout retains the original local commit author and message for "Recipes/Pasta.md"
    And the cloned checkout file "Recipes/Pasta.md" is:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Finish in the sauce
      """
    And note "Pasta" should have content "Salt the water first"
    When I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the resolved local head as the accepted head
    And I should see note "CLI Clone Notebook/Recipes/Pasta" has content "Finish in the sauce"
