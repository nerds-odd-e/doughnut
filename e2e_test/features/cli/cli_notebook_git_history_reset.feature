@bundleCliE2eInstall
@withCliConfig
Feature: CLI notebook Git history reset
  As a learner whose notebook content is no longer in its accepted Git history, I want to reset
  the notebook's Git history from its settings so the whole notebook can be cloned again.

  Background:
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And I am logged in as an existing user
    And I have a notebook "CLI Reset Notebook"
    And I have a note "Overview" under notebook "CLI Reset Notebook" with content:
      """
      ---
      type: Note
      ---
      """
    And the notebook "CLI Reset Notebook" has readme content "Notebook landing"
    And I have a valid Donut Access Token with label "E2E CLI Reset Token"

  Scenario: Resetting Git history makes the whole notebook clonable in one commit
    When I reset the Git history of notebook "CLI Reset Notebook" from its settings
    And I clone the notebook "CLI Reset Notebook" into a temporary destination using the installed CLI
    Then the cloned checkout is a clean single-commit checkout on branch "main"
    And the cloned checkout contains exactly:
      | README.md   |
      | Overview.md |
    And the cloned checkout file ".gitattributes" is:
      """
      * filter=lfs diff=lfs merge=lfs -text
      *.md !filter !diff !merge text
      .gitattributes !filter !diff !merge text
      .keep !filter !diff !merge text
      **/.keep !filter !diff !merge text

      """
