Feature: Notebook export

  Background:
    Given I am logged in as an existing user
    And I have a notebook "E2E Export Notebook" with a note "Export Root Note"

  Scenario: Export notebook downloads a zip
    When I export notebook "E2E Export Notebook" from the catalog
    Then a zip file for notebook "E2E Export Notebook" should be downloaded

  Scenario: Exported zip writes non-blank readme as README.md
    And the notebook "E2E Export Notebook" has readme content "Notebook landing"
    And the notebook "E2E Export Notebook" has a readme-only folder "Has Readme" with readme "Folder landing"
    And I have a note "Blank folder note" under notebook "E2E Export Notebook" in folder "Blank Readme" with content:
      """
      in blank folder
      """
    When I export notebook "E2E Export Notebook" from the catalog
    Then the downloaded zip for notebook "E2E Export Notebook" contains "README.md"
    And the downloaded zip entry "README.md" of notebook "E2E Export Notebook" includes "type: Readme"
    And the downloaded zip entry "README.md" of notebook "E2E Export Notebook" includes "Notebook landing"
    And the downloaded zip for notebook "E2E Export Notebook" contains "Has Readme/README.md"
    And the downloaded zip for notebook "E2E Export Notebook" does not contain "index.md"
    And the downloaded zip for notebook "E2E Export Notebook" does not contain "Has Readme/index.md"
    And the downloaded zip for notebook "E2E Export Notebook" does not contain "Blank Readme/README.md"

  Scenario: Collision filename is a human sequence and carries display title
    And I have a note "Recipe" under notebook "E2E Export Notebook" with content:
      """
      first recipe
      """
    And I have a note "Recipe*" under notebook "E2E Export Notebook" with content:
      """
      starred recipe
      """
    When I export notebook "E2E Export Notebook" from the catalog
    Then the downloaded zip for notebook "E2E Export Notebook" contains "Recipe (2).md"
    And the downloaded zip entry "Recipe.md" of notebook "E2E Export Notebook" does not include "title:"
    And the downloaded zip entry "Recipe (2).md" of notebook "E2E Export Notebook" includes "title: Recipe*"

  Scenario: Exported note file is stored markdown without a generated title heading
    And I have a note "Pasta" under notebook "E2E Export Notebook" with content:
      """
      ---
      type: Note
      ---
      # Author heading

      Boil water
      """
    When I export notebook "E2E Export Notebook" from the catalog
    Then the downloaded zip entry "Pasta.md" of notebook "E2E Export Notebook" includes "type: Note"
    And the downloaded zip entry "Pasta.md" of notebook "E2E Export Notebook" includes "# Author heading"
    And the downloaded zip entry "Pasta.md" of notebook "E2E Export Notebook" includes "Boil water"
    And the downloaded zip entry "Pasta.md" of notebook "E2E Export Notebook" does not include "# Pasta"
