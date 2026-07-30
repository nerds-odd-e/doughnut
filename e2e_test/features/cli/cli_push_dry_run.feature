# Phases 1-3 of docs/plans/2026-07-29-cli-push-dry-run-preview.md. Story #5 of
# .planning/notes/2026-07-24-portable-notebook-workspace.md ("preview local
# edits and conflicts before pushing"). Mirrors cli_sync_dry_run.feature's
# background and step reuse. The first preview in a workspace shows a plain
# diff and never touches anything but its own baseline bookkeeping file; a
# later preview reads that baseline to say which side changed, or to call out a
# conflict when both sides changed and diverged since the baseline.
#
# An unlabeled, `(pull)` or `(CONFLICT)` diff reads workspace-to-notebook, the
# same way `/sync --dry-run` reads. A `(push)` diff reads notebook-to-workspace
# instead — removed is Doughnut as it stands, added is the workspace as it
# stands — so it shows what pushing would actually write into Doughnut. Because
# that direction flips, every diff names its own sides `git diff` style:
# `--- <side the removed lines come from>` / `+++ <side the added lines come
# from>`.
#
# Each scenario asserts the path header with its two side headers, then the
# changed content with the count, as two blocks rather than one. As
# cli_sync_dry_run.feature also notes, an exported note carries frontmatter and
# a `# title` heading above its content, so a body-only change prints those as
# context lines and the two blocks are not adjacent in the real output. Naming
# the sides is the point here, so the blocks that bracket that preamble are
# asserted and the preamble itself is left to the unit tests, which is where a
# change to the export's shape belongs.
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
        less.md
          --- workspace
          +++ Doughnut
        """
      And I should see the preview in past CLI assistant messages:
        """
          - Hello
          + Hello world!

        1 note would change.
        """

    Scenario: First preview with nothing different
      When I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then I should see "No changes to push." in past CLI assistant messages

  Rule: A later preview says which side changed since the last one

    Background:
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI
      And I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI

    Scenario: A note changed only in Doughnut would come in on a pull
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md (pull)
          --- workspace
          +++ Doughnut
        """
      And I should see the preview in past CLI assistant messages:
        """
          - Hello
          + Hello world!

        1 note would change.
        """

    Scenario: A note changed only in the workspace would go out on a push
      When I edit the content of "less.md" in the workspace "./BenNotebook" to "Hello from Obsidian"
      And I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md (push)
          --- Doughnut
          +++ workspace
        """
      And I should see the preview in past CLI assistant messages:
        """
          - Hello
          + Hello from Obsidian

        1 note would change.
        """

    Scenario: A note changed on both sides since the last preview is a conflict
      When I edit the content of "less.md" in the workspace "./BenNotebook" to "Hello from Obsidian"
      And the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md (CONFLICT)
          --- workspace
          +++ Doughnut
        """
      And I should see the preview in past CLI assistant messages:
        """
          - Hello from Obsidian
          + Hello world!

        1 conflict.
        """

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
