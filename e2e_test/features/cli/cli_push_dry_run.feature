# Phase 1 of docs/plans/2026-07-29-cli-push-dry-run-preview.md. Story #5 of
# .planning/notes/2026-07-24-portable-notebook-workspace.md ("preview local
# edits and conflicts before pushing"). Mirrors cli_sync_dry_run.feature's
# background and step reuse. Distinguishing which side changed (pull-suggested
# vs push-suggested vs conflict) is Phases 2-3, not covered here yet — this
# phase only proves the command exists, shows a plain diff on first use, and
# never touches anything but its own baseline bookkeeping file.
#
# Diff formatting, classification, and argument parsing are covered by the CLI
# unit tests: cli/tests/previewPush.test.ts, cli/tests/pushArgument.test.ts.
# These scenarios verify only the CLI -> API -> diff -> output integration
# path those unit tests cannot reach.
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Preview what a push would change

  As a notebook owner
  I want to preview local edits before pushing
  So that I can review them before any push happens

  `/push --dry-run` never writes to Doughnut and never writes any `.md` file in
  the workspace. Its only mutation is its own `.doughnut-sync/baseline.json`
  bookkeeping file, used to tell local and remote changes apart on later runs.

  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI

  Rule: The first preview in a workspace reports the current difference

    Background:
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: First preview reports a changed note
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
          - Hello
          + Hello world!

        1 note would change.
        """

    Scenario: First preview with nothing different
      When I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then I should see "No changes to push." in past CLI assistant messages

  Rule: The preview leaves the workspace and Doughnut untouched

    Background:
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: A note's file is not written by the preview
      When the note "less" is changed in Doughnut to "Hi world!"
      And I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then the file "less.md" in the workspace "./BenNotebook" should hold "Hello"

    Scenario: The preview's only addition is its own baseline file
      When I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then the workspace "./BenNotebook" should hold only:
        | Path                          |
        | less.md                       |
        | .doughnut-sync/baseline.json  |

    Scenario: The remote note is not modified by the preview
      When the note "less" is changed in Doughnut to "Hi world!"
      And I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then the note "less" in Doughnut should still hold "Hi world!"
