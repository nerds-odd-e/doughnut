# Agreed at the refinement session on 2026-07-27; see
# docs/refinement/2026-07-27/QUESTIONS-for-export-team.md ("Already agreed").
#
# Filename parsing, path safety, argument parsing, and summary wording are
# covered by the CLI unit tests:
#   cli/tests/writeNotebookExport.test.ts
#   cli/tests/contentDispositionFileName.test.ts
#
# The scenarios below verify only what the unit tests cannot reach: the full
# CLI -> API -> unzip -> filesystem path against a real notebook.
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Export a notebook to a local Markdown tree

  As a notebook owner
  I want to write the active notebook into a directory I choose
  So that I can read and edit my notes with ordinary Markdown tools

  The export runs inside the notebook context that /use establishes, and
  writes a subdirectory named after the notebook, so exporting several
  notebooks into one folder keeps them apart.

  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI

  Scenario: Export a notebook into an empty directory
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And an empty export destination "./ExportTarget"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I export the notebook into "./ExportTarget"
    Then the export destination "./ExportTarget" should hold only:
      | Path                 |
      | Ben Notebook/less.md |
