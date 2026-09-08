@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook web-created note
  As a notebook owner, I want to pull a note I created on the web into my local Git checkout so I
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
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
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

  Scenario: Publishing a local refinement of received web-created note text updates Donut
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I create a title-only root note titled "Shopping list" in the notebook "CLI Clone Notebook"
    And I view the note content as rich content
    And I update note "Shopping list" content to become "Milk and eggs"
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
      Milk and eggs
      """
    When I commit the following edit to "Shopping list.md" in the cloned checkout:
      """
      ---
      type: Note
      ---
      Milk, eggs, and bread
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And I should see note "CLI Clone Notebook/Shopping list" has content "Milk, eggs, and bread"

  Scenario: Pulling a web-created Shopping list while retaining a local Pasta edit then publishing
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit the following edit to "Recipes/Pasta.md" in the cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And I create a title-only root note titled "Shopping list" in the notebook "CLI Clone Notebook"
    When I pull the cloned checkout using the installed CLI
    Then the cloned checkout is a clean rebased child of the accepted head
    And the cloned checkout retains the original local commit for "Recipes/Pasta.md"
    And "Shopping list.md" in the cloned checkout matches the accepted parent
    And the cloned checkout contains exactly:
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
    And I should see note "CLI Clone Notebook/Shopping list" has content ""

  Scenario: Pulling a web-created then saved Shopping list while retaining a local Pasta edit
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit the following edit to "Recipes/Pasta.md" in the cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And I create a title-only root note titled "Shopping list" in the notebook "CLI Clone Notebook"
    And I view the note content as rich content
    And I update note "Shopping list" content to become "Milk and eggs"
    When I pull the cloned checkout using the installed CLI
    Then the cloned checkout is a clean rebased child of the accepted head
    And the cloned checkout retains the original local commit for "Recipes/Pasta.md"
    And "Shopping list.md" in the cloned checkout matches the accepted parent
    And the cloned checkout contains exactly:
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
      Milk and eggs
      """
    And the cloned checkout file "Recipes/Pasta.md" is:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente

      """
    And note "Pasta" should have content "Boil water"
