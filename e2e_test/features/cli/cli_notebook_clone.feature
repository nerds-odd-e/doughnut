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
    And the notebook "CLI Clone Notebook" has an empty folder "Ideas"
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
      | Ideas/.keep        |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
    When I open the notebook "CLI Clone Notebook" from the notebook catalog
    Then the notebook readme body includes "Notebook landing"

  Scenario: Publishing an initial nested tree round-trips the authored checkout
    Given I have a notebook "CLI Initial Tree Notebook"
    And the notebook "CLI Initial Tree Notebook"'s Git binding reflects its current content
    When I clone the notebook "CLI Initial Tree Notebook" into a temporary destination using the installed CLI
    And I author and commit the following initial tree in the cloned checkout:
      | path                    | content                                                                                                                |
      | Overview.md             | ---\ntype: Note\n---\nWeekly meal plan                                                                                  |
      | Recipes/README.md       | ---\ntype: Readme\n---\nRecipes from my kitchen                                                                          |
      | Recipes/Details/Pasta.md | ---\ntype: Note\nauthor: Chef Boyardee\n---\nSimmer until al dente                                                        |
      | Links.md                | ---\ntype: Relationship\nrelation: related-to\nsource: "[[Overview]]"\ntarget: "[[Recipes/Details/Pasta]]"\n---\nDinner plan |
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I clone the notebook "CLI Initial Tree Notebook" into a second temporary destination using the installed CLI
    Then the second cloned checkout is a clean checkout of the accepted head
    And the second cloned checkout contains exactly:
      | Overview.md             |
      | Recipes/README.md       |
      | Recipes/Details/Pasta.md |
      | Links.md                |
    And the second cloned checkout file "Overview.md" is:
      """
      ---
      type: Note
      ---
      Weekly meal plan

      """
    And the second cloned checkout file "Recipes/README.md" is:
      """
      ---
      type: Readme
      ---
      Recipes from my kitchen

      """
    And the second cloned checkout file "Recipes/Details/Pasta.md" is:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente

      """
    And the second cloned checkout file "Links.md" is:
      """
      ---
      type: Relationship
      relation: related-to
      source: "[[Overview]]"
      target: "[[Recipes/Details/Pasta]]"
      ---
      Dinner plan

      """

  Scenario: Publishing a committed root readme edit updates the notebook description and round-trips
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit the following edit to "README.md" in the cloned checkout:
      """
      ---
      type: Readme
      author: owner
      ---
      Updated notebook landing
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I open the notebook "CLI Clone Notebook" from the notebook catalog
    Then the notebook readme body includes "Updated notebook landing"
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout is a clean checkout of the accepted head
    And the second cloned checkout file "README.md" is:
      """
      ---
      type: Readme
      author: owner
      ---
      Updated notebook landing

      """

  Scenario: Publishing a committed folder readme edit updates the folder description and round-trips
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit the following edit to "Recipes/README.md" in the cloned checkout:
      """
      ---
      type: Readme
      author: owner
      ---
      Updated folder landing
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I open the notebook "CLI Clone Notebook" from the notebook catalog
    And I open the folder page for "Recipes" from the sidebar
    Then the folder readme should contain "Updated folder landing"
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout is a clean checkout of the accepted head
    And the second cloned checkout file "Recipes/README.md" is:
      """
      ---
      type: Readme
      author: owner
      ---
      Updated folder landing

      """

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
    Then I should see "one or more edited existing ordinary Markdown notes at unchanged paths" in the non-interactive output
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
