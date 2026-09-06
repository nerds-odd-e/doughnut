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
    And the notebook "CLI Clone Notebook"'s Git binding reflects its current content
    And I have a valid Donut Access Token with label "E2E CLI Clone Token"

  Scenario: Cloning an owned notebook produces a clean canonical Git checkout
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    Then the cloned checkout is a clean single-commit checkout on branch "main"
    And the cloned checkout contains exactly:
      | README.md         |
      | Overview.md       |
      | Recipes/README.md |
      | Recipes/Pasta.md  |
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

  Scenario: Publishing a committed new note makes it available at its authored path
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    Then I should see "one added or edited Markdown note at the notebook root or in an existing folder" in the non-interactive output
    When I add and commit the following note at "Recipes/Pantry Staples.md" in the cloned checkout:
      """
      ---
      type: Note
      ---
      Keep semolina pasta stocked.
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Recipes/Pantry Staples" has content "Keep semolina pasta stocked."
