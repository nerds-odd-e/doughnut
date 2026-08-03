# Specified at the refinement session on 2026-07-27; see
# docs/refinement/2026-07-27/SPEC-sync-pull.md.
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Pull remote note changes into a workspace

  As a notebook owner
  I want `/sync` to create, update, and move local Markdown files from the remote notebook
  So that remote edits in Doughnut reach my Markdown workspace without disturbing other files

  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI

  Scenario: Pull updates one remote change
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When the note "less" is changed in Doughnut to "Hello world!"
    And I pull into the workspace "./BenNotebook"
    Then I should see "1 note updated." in past CLI assistant messages
    And the file "less.md" in the workspace "./BenNotebook" should hold "Hello world!"
    And the workspace "./BenNotebook" should hold only:
      | Path                         |
      | less.md                      |
      | .doughnut-sync/baseline.json |

  Scenario: Extra local-only file is untouched
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
    And the workspace "./BenNotebook" has an extra file "Less 2.md" with content:
      """
      local only
      """
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When the note "less" is changed in Doughnut to "Hello world!"
    And I pull into the workspace "./BenNotebook"
    Then the file "less.md" in the workspace "./BenNotebook" should hold "Hello world!"
    And the file "Less 2.md" in the workspace "./BenNotebook" should hold "local only"

  Scenario: Pull creates a remote-only note
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
      | scrum | Sprint  |
    And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
    And the file "scrum.md" is removed from the workspace "./BenNotebook"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When the note "scrum" is changed in Doughnut to "Sprint review"
    And I pull into the workspace "./BenNotebook"
    Then I should see "1 note updated." in past CLI assistant messages
    And the file "scrum.md" in the workspace "./BenNotebook" should hold "Sprint review"
    And the file "less.md" in the workspace "./BenNotebook" should hold "Hello"
    And the workspace "./BenNotebook" should hold only:
      | Path                         |
      | less.md                      |
      | scrum.md                     |
      | .doughnut-sync/baseline.json |

  Scenario: No-op when already in sync
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I pull into the workspace "./BenNotebook"
    Then I should see "No changes to pull." in past CLI assistant messages
    And the file "less.md" in the workspace "./BenNotebook" should hold "Hello"
    And the workspace "./BenNotebook" should not contain ".doughnut-sync/baseline.json"

@perfSync
  Scenario: One change among 1000 notes completes within 5 seconds
    Given I have a notebook "Perf Notebook" with 1000 numbered notes
    And the workspace "./PerfNotebook" holds the same content as "Perf Notebook"
    And I enter the slash command "/use Perf Notebook" in the interactive CLI
    When the note "note-0500" is changed in Doughnut to "changed body"
    Then pulling into the workspace "./PerfNotebook" should complete within 5 seconds
    And the file "note-0500.md" in the workspace "./PerfNotebook" should hold "changed body"
