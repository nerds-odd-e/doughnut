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
      | Path                                      |
      | Ben Notebook/.doughnut-sync/baseline.json |
      | Ben Notebook/less.md                      |

  Scenario: Export preserves folder structure and note bodies
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content         | Folder         |
      | intro | Hello from root |                |
      | team  | Sprint          | LeSS in Action |
    And the notebook "Ben Notebook" has readme content "About this notebook"
    And an empty export destination "./ExportTarget"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I export the notebook into "./ExportTarget"
    Then the export destination "./ExportTarget" should hold only:
      | Path                                      |
      | Ben Notebook/.doughnut-sync/baseline.json |
      | Ben Notebook/index.md                     |
      | Ben Notebook/intro.md                     |
      | Ben Notebook/LeSS in Action/team.md       |
    And the file "Ben Notebook/LeSS in Action/team.md" in the export destination "./ExportTarget" should hold "Sprint"

  Scenario: Exporting again reflects a changed note
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And an empty export destination "./ExportTarget"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    And I export the notebook into "./ExportTarget"
    When the note "less" is changed in Doughnut to "Hello world!"
    And I export the notebook into "./ExportTarget"
    Then the export destination "./ExportTarget" should hold only:
      | Path                                      |
      | Ben Notebook/.doughnut-sync/baseline.json |
      | Ben Notebook/less.md                      |
    And the file "Ben Notebook/less.md" in the export destination "./ExportTarget" should hold "Hello world!"

  Scenario: An unrelated file in the destination survives an export
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And an empty export destination "./ExportTarget"
    And the export destination "./ExportTarget" has an extra file "Ben Notebook/scratch.md" with content:
      """
      keep me
      """
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I export the notebook into "./ExportTarget"
    Then the export destination "./ExportTarget" should hold only:
      | Path                                      |
      | Ben Notebook/.doughnut-sync/baseline.json |
      | Ben Notebook/less.md                      |
      | Ben Notebook/scratch.md                   |
    And the file "Ben Notebook/scratch.md" in the export destination "./ExportTarget" should hold "keep me"

  Scenario: A destination that does not exist reports a readable error
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I export the notebook into "./NoSuchDirectory"
    Then I should see "No directory at" in past CLI assistant messages

  Scenario: Export includes doughnut_id frontmatter on each note
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And an empty export destination "./ExportTarget"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I export the notebook into "./ExportTarget"
    Then the file "Ben Notebook/less.md" in the export destination "./ExportTarget" should hold "doughnut_id:"

  Scenario: Export rewrites resolvable wiki links to ordinary Markdown links
    Given I have a notebook "Ben Notebook" with notes:
      | Title  | Content        |
      | source | See [[target]] |
      | target | Target body    |
    And an empty export destination "./ExportTarget"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I export the notebook into "./ExportTarget"
    Then the file "Ben Notebook/source.md" in the export destination "./ExportTarget" should hold "]("
    And the file "Ben Notebook/source.md" in the export destination "./ExportTarget" should hold "target.md"

  Scenario: Export rewrites attachment refs to absolute remote URLs
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | photo | Hello   |
    And I upload an image from fixture "moon.jpg" to the note "photo"
    And an empty export destination "./ExportTarget"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I export the notebook into "./ExportTarget"
    Then the file "Ben Notebook/photo.md" in the export destination "./ExportTarget" should hold "http"
    And the file "Ben Notebook/photo.md" in the export destination "./ExportTarget" should hold "/attachments/images/"
