@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook clone
  As a learner, I want to clone my notebook to a local Git checkout using the Donut CLI so I
  can read and edit it with ordinary tools.

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
    And the readme of "CLI Clone Notebook" in Donut should be "Notebook landing"

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

  Scenario: Publishing committed readme edits update notebook/folder descriptions and round-trips
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit the following document changes together in the cloned checkout:
      | path              | content                                                              |
      | README.md         | ---\ntype: Readme\nauthor: owner\n---\nUpdated notebook landing      |
      | Recipes/README.md | ---\ntype: Readme\nauthor: owner\n---\nUpdated folder landing        |
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the readme of "CLI Clone Notebook" in Donut should be "Updated notebook landing"
    And the readme of "CLI Clone Notebook/Recipes" in Donut should be "Updated folder landing"
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
    And the second cloned checkout file "Recipes/README.md" is:
      """
      ---
      type: Readme
      author: owner
      ---
      Updated folder landing

      """
