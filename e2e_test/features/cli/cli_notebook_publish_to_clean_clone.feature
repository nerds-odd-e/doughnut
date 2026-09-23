@bundleCliE2eInstall @withCliConfig
Feature: CLI notebook publish received by a clean clone
  As a notebook owner, I want a clean clone to receive my published local changes so both copies retain my work.

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

  Scenario: Publishing a folder README and notes together is received by a clean clone
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I create a title-only root note titled "Shopping list" in the notebook "CLI Clone Notebook"
    And I pull the cloned checkout using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit the following document changes together in the cloned checkout:
      | path           | content                                      |
      | 例文/README.md | ---\ntype: Readme\n---\nExample sentences     |
      | 例文/A.md      | ---\ntype: Note\n---\nFirst example             |
      | 例文/B.md      | ---\ntype: Note\n---\nSecond example            |
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout contains exactly:
      | README.md          |
      | Overview.md        |
      | Shopping list.md   |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
      | 例文/README.md      |
      | 例文/A.md           |
      | 例文/B.md           |
    And the second cloned checkout retains its original head as an ancestor
    And the second cloned checkout is a clean checkout of the accepted head
    And the second cloned checkout file "例文/README.md" is:
      """
      ---
      type: Readme
      ---
      Example sentences

      """
    And the second cloned checkout file "例文/A.md" is:
      """
      ---
      type: Note
      ---
      First example

      """
    And the second cloned checkout file "例文/B.md" is:
      """
      ---
      type: Note
      ---
      Second example

      """
  Scenario: Publishing a note rename with an unrelated edit is received by a clean clone
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit a rename of "Recipes/Pasta.md" to "Recipes/Pasta basics.md" and the following unrelated edit to "Overview.md" together in the cloned checkout:
      """
      ---
      type: Note
      ---
      Composed publication received
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I pull the second cloned checkout using the installed CLI
    Then the published checkout has exactly one commit after the second clone original head
    And the second cloned checkout retains its original head as an ancestor
    And the second cloned checkout is a clean checkout of the accepted head
    And the second cloned checkout contains exactly:
      | README.md                 |
      | Overview.md               |
      | Kitchen/README.md         |
      | Recipes/README.md         |
      | Recipes/Pasta basics.md   |
    And the second cloned checkout file "Recipes/Pasta basics.md" is:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Boil water
      """
    And the second cloned checkout file "Overview.md" is:
      """
      ---
      type: Note
      ---
      Composed publication received

      """

  Scenario: Publishing an accumulated same-note rename and edit session is received by a clean clone
    Given I capture the note id of "Pasta"
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit a rename of "Recipes/Pasta.md" to "Recipes/Pasta basics.md" and the following edit to the renamed note together in the cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente
      """
    And I commit the following edit to "Recipes/Pasta basics.md" in the cloned checkout:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente and salt the water
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout preserves the publisher's A to C history
    And the second cloned checkout retains its original head as an ancestor
    And the second cloned checkout is a clean checkout of the accepted head
    And the second cloned checkout contains exactly:
      | README.md                 |
      | Overview.md               |
      | Kitchen/README.md         |
      | Recipes/README.md         |
      | Recipes/Pasta basics.md   |
    And the second cloned checkout file "Recipes/Pasta basics.md" is:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Simmer until al dente and salt the water

      """
    And I open the original note route
    And the note content on the current page should be "Simmer until al dente and salt the water"
  Scenario: Published root files reach another checkout byte for byte
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit the root file "reference.json" and the root file "diagram.png" holding the bytes "89 FF FE 00" together in the cloned checkout:
      """
      {"kind":"reference","pages":3}
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I update note "Overview" with content "Reviewed on the web"
    And I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout contains exactly:
      | README.md          |
      | Overview.md        |
      | diagram.png        |
      | reference.json     |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
    And the second cloned checkout retains its original head as an ancestor
    And the second cloned checkout is a clean checkout of the notebook "CLI Clone Notebook" accepted head
    And the second cloned checkout file "diagram.png" holds the bytes "89 FF FE 00"
    And the second cloned checkout file "reference.json" holds exactly:
      """
      {"kind":"reference","pages":3}

      """
    And the second cloned checkout file "Overview.md" is:
      """
      ---
      type: Note
      ---
      Reviewed on the web
      """
    When I pull the cloned checkout using the installed CLI
    And I commit the following edit to "reference.json" in the cloned checkout:
      """
      {"kind":"reference","pages":4}
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I clone the notebook "CLI Clone Notebook" into a fresh temporary destination using the installed CLI
    Then the fresh clone contains exactly:
      | README.md          |
      | Overview.md        |
      | diagram.png        |
      | reference.json     |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
    And the fresh clone is a clean checkout of the accepted head
    And the fresh clone file "diagram.png" holds the bytes "89 FF FE 00"
    And the fresh clone file "reference.json" holds exactly:
      """
      {"kind":"reference","pages":4}

      """

  Scenario: Nested attachments follow a web folder rename into a clean clone
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Clone Notebook" into a second temporary destination using the installed CLI
    And I commit the root file "References/reference.json" and the root file "References/diagram.png" holding the bytes "89 FF FE 00" together in the cloned checkout:
      """
      {"kind":"reference","pages":3}
      """
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I open the folder page for "References" in notebook "CLI Clone Notebook"
    And I rename the folder heading to "Research"
    And I pull the cloned checkout using the installed CLI
    And I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout preserves the publisher's A to C history
    And the second cloned checkout retains its original head as an ancestor
    And the second cloned checkout is a clean checkout of the notebook "CLI Clone Notebook" accepted head
    And the second cloned checkout contains exactly:
      | README.md                 |
      | Overview.md               |
      | Research/diagram.png      |
      | Research/reference.json   |
      | Kitchen/README.md         |
      | Recipes/README.md         |
      | Recipes/Pasta.md          |
    And the second cloned checkout file "Research/diagram.png" holds the bytes "89 FF FE 00"
    And the second cloned checkout file "Research/reference.json" holds exactly:
      """
      {"kind":"reference","pages":3}

      """
