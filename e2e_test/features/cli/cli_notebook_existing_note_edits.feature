@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook existing-note edits
  As a notebook owner, I want to publish related edits to existing notes as one commit so Donut
  updates those notes and another checkout can pull the same revision.

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

  Scenario: Publishing related edits to existing notes updates Donut and a second checkout
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit the following related edits together in the cloned checkout:
      | path             | content |
      | Overview.md      | ---\ntype: Note\n---\nWeekly meal plan |
      | Recipes/Pasta.md | ---\ntype: Note\nauthor: Marcella Hazan\n---\nSimmer until al dente |
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Overview" has content "Weekly meal plan"
    And I should see note "CLI Clone Notebook/Recipes/Pasta" has content "Simmer until al dente"
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout is a clean checkout of the accepted head
    And the second cloned checkout file "Overview.md" is:
      """
      ---
      type: Note
      ---
      Weekly meal plan

      """
    And the second cloned checkout file "Recipes/Pasta.md" is:
      """
      ---
      type: Note
      author: Marcella Hazan
      ---
      Simmer until al dente

      """
