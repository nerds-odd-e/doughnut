@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook folder relocation
  As a notebook owner, I want to publish one local folder move through the installed CLI so Donut
  keeps the same folder and note identities at the new path, and a later content edit updates that note.

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

  Scenario: Publishing a committed folder relocation updates the same Donut note at the new folder path
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit a rename of "Recipes" to "Kitchen/Recipes" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Kitchen/Recipes/Pasta" has content "Boil water"

  Scenario: Pulling a clean checkout receives an accepted folder relocation
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit a rename of "Recipes" to "Kitchen/Recipes" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout contains exactly:
      | README.md                     |
      | Overview.md                   |
      | Kitchen/README.md             |
      | Kitchen/Recipes/README.md     |
      | Kitchen/Recipes/Pasta.md      |
    And the second cloned checkout retains its original head as an ancestor

  Scenario: Publishing a Pasta edit after receiving an accepted folder relocation updates the same Donut note
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit a rename of "Recipes" to "Kitchen/Recipes" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I pull the second cloned checkout using the installed CLI
    And I commit the following edit to "Kitchen/Recipes/Pasta.md" in the second cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And I publish the second cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Kitchen/Recipes/Pasta" has content "Simmer until al dente"
