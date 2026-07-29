# Specified at the refinement session on 2026-07-27; see
# docs/refinement/2026-07-27/SPEC-sync-dry-run.md.
#
# Diff formatting (context lines, hunks, blank lines, Markdown markup),
# report assembly (multiple notes, ordering, folder paths), and argument
# parsing are covered by the CLI unit tests:
#   cli/tests/unifiedDiff.test.ts
#   cli/tests/previewPull.test.ts
#   cli/tests/syncArgument.test.ts
#
# The e2e scenarios below verify only integration-level concerns that
# the unit tests cannot reach: the full CLI → API → diff → output path,
# workspace safety, and error handling that needs server-side state.
#
# An exported note carries frontmatter and a `# title` heading above its
# content, so a diff of the content is preceded by those as context lines.
# These scenarios assert the changed content and the count, which is what
# reaching the real export proves; the surrounding context is unit-tested.
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Preview what a pull would change

  As a notebook owner
  I want to preview the differences between my workspace and the notebook
  So that I can review them before any local file is written

  The preview runs inside the notebook context that /use establishes. Each run
  exports the notebook afresh and compares the workspace against it, keeping
  nothing between runs. Removed lines are the workspace as it stands; added
  lines are the notebook as it stands.

  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI

  Rule: A changed note is reported as a diff

    Scenario: Preview one changed note
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see "less.md" in past CLI assistant messages
      And I should see the preview in past CLI assistant messages:
        """
          - Hello
          + Hello world!

        1 note would change.
        """

  Rule: A difference is reported whichever side it came from

    Background:
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: A note edited locally is reported as what a pull would overwrite
      When I edit the content of "less.md" in the workspace "./BenNotebook" to "Hello from Obsidian"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see "less.md" in past CLI assistant messages
      And I should see the preview in past CLI assistant messages:
        """
          - Hello from Obsidian
          + Hello

        1 note would change.
        """

    Scenario: No difference to report
      When I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see "No changes to pull." in past CLI assistant messages

  Rule: The preview leaves nothing behind

    Background:
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: The workspace is not written to
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then the file "less.md" in the workspace "./BenNotebook" should hold "Hello"

    Scenario: The preview adds no files of its own
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then the workspace "./BenNotebook" should hold only:
        | Path    |
        | less.md |

