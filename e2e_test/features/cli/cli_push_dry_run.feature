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

  Rule: Exporting primes the baseline, so the very next preview can already tell a direction

    Scenario: A note edited right after export is labeled a push without a priming run
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And an empty export destination "./ExportTarget"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI
      And I export the notebook into "./ExportTarget"
      And the workspace "./BenNotebookExport" is the notebook "Ben Notebook" exported into "./ExportTarget"
      When I edit the content of "less.md" in the workspace "./BenNotebookExport" to "Hello from Obsidian"
      And I enter the slash command "/push --dry-run ./BenNotebookExport" in the interactive CLI
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
