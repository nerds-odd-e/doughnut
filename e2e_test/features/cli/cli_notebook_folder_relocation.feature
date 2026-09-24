@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook folder relocation
  As a notebook owner, I want to publish one local folder move through the installed CLI so Donut
  keeps the same folder and note identities at the new path, and a later content edit updates that note.

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

  Scenario: Publishing a Pasta edit committed before an accepted folder relocation updates the same Donut note
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit the following edit to "Recipes/Pasta.md" in the second cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And I commit a rename of "Recipes" to "Kitchen/Recipes" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I pull the second cloned checkout using the installed CLI
    And I publish the second cloned checkout using the installed CLI
    Then the installed CLI reports the rebased local head as the accepted head
    And I should see note "CLI Clone Notebook/Kitchen/Recipes/Pasta" has content "Simmer until al dente"

  Scenario: Publishing relocate then descendant edit and add commits retains identities and history
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit a rename of "Recipes" to "Kitchen/Recipes" in the cloned checkout
    And I commit the following document changes together in the cloned checkout:
      | path                         | content |
      | Kitchen/Recipes/Pasta.md     | ---\ntype: Note\nauthor: Chef Boyardee\n---\nSimmer until al dente |
      | Kitchen/Recipes/Sauce.md     | ---\ntype: Note\n---\nTomato base |
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Kitchen/Recipes/Pasta" has content "Simmer until al dente"
    And I should see note "CLI Clone Notebook/Kitchen/Recipes/Sauce" has content "Tomato base"
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout preserves the publisher's A to C history
    And the second cloned checkout contains exactly:
      | README.md                     |
      | Overview.md                   |
      | Kitchen/README.md             |
      | Kitchen/Recipes/README.md     |
      | Kitchen/Recipes/Pasta.md      |
      | Kitchen/Recipes/Sauce.md      |
    And the second cloned checkout file "Kitchen/Recipes/Pasta.md" is:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente

      """

  Scenario: Publishing a folder subtree trash round trip is received by a later pull
    Given I have a notebook "CLI Folder Trash Notebook"
    And the notebook "CLI Folder Trash Notebook" has a readme-only folder "Recipes" with readme "Folder landing"
    And I have a note "Pasta" under notebook "CLI Folder Trash Notebook" in folder "Recipes" with content:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Boil water
      """
    And I have a note "Sauce" under notebook "CLI Folder Trash Notebook" in folder "Recipes" with content:
      """
      ---
      type: Note
      ---
      Tomato base
      """
    And the notebook "CLI Folder Trash Notebook" has a folder "Empty" under note "Pasta"
    And the notebook "CLI Folder Trash Notebook" has an empty folder "_trash"
    And the notebook "CLI Folder Trash Notebook"'s Git binding reflects its current content
    When I clone the notebook "CLI Folder Trash Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Folder Trash Notebook" into a second temporary destination using the installed CLI
    And I commit a rename of "Recipes" to "_trash/Recipes" and a removal of "_trash/.keep" together in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I commit a rename of "_trash/Recipes" to "Recipes" and an empty keep at "_trash/.keep" together in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Folder Trash Notebook/Recipes/Pasta" has content "Boil water"
    And I should see note "CLI Folder Trash Notebook/Recipes/Sauce" has content "Tomato base"
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout contains exactly:
      | Recipes/README.md     |
      | Recipes/Pasta.md      |
      | Recipes/Sauce.md      |
      | Recipes/Empty/.keep   |
      | _trash/.keep          |
    And the second cloned checkout retains its original head as an ancestor
