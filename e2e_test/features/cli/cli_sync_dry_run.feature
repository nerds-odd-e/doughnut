@ignore
# Specified at the refinement session on 2026-07-27; see
# docs/refinement/2026-07-27/SPEC-sync-dry-run.md.
# Ignored until the implementation lands: each scenario is enabled in the commit
# that makes it pass.
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Preview what a pull would change

  As a notebook owner
  I want to preview the differences between my workspace and the notebook
  So that I can review them before any local file is written

  The preview runs inside the notebook context that /use establishes. Each run
  exports the notebook into a scratch directory, compares the workspace against
  it, and discards the scratch directory. Removed lines are the workspace as it
  stands; added lines are the notebook as it stands.

  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI

  Rule: A changed note is reported as a diff

    Background:
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: Preview one changed note
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md
          - Hello
          + Hello world!

        1 note would change.
        """

    Scenario: Preview 2 changes
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
        | scrum | Sprint  |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "less" is changed in Doughnut to "Hello world!"
      And the note "scrum" is changed in Doughnut to "Sprint review"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md
          - Hello
          + Hello world!

        scrum.md
          - Sprint
          + Sprint review

        2 notes would change.
        """

    Scenario: An unchanged note is not reported
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
        | scrum | Sprint  |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see "less.md" in past CLI assistant messages
      And I should not see "scrum.md" in past CLI assistant messages

  Rule: The reported path shows where the note sits in the workspace

    Background:
      Given I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: Change a note in a folder
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Folder         | Content |
        | intro |                | Hello   |
        | team  | LeSS in Action | Sprint  |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to "Sprint review"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        LeSS in Action/team.md
          - Sprint
          + Sprint review

        1 note would change.
        """

    Scenario: Multiple folders, ordered by path
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Folder         | Content |
        | intro |                | Hello   |
        | team  | LeSS in Action | Sprint  |
        | tech  | Engineering    | Trunk   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to "Sprint review"
      And the note "tech" is changed in Doughnut to "Trunk based"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        Engineering/tech.md
          - Trunk
          + Trunk based

        LeSS in Action/team.md
          - Sprint
          + Sprint review

        2 notes would change.
        """

  Rule: A diff carries up to three unchanged lines around each change

    Background:
      Given I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: One line changed in a note of many lines
      Given I have a notebook "Ben Notebook" with note "team" holding:
        """
        Sprint planning
        Daily standup
        Two week sprint
        Retrospective
        Demo
        """
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to:
        """
        Sprint planning
        Daily standup
        Three week sprint
        Retrospective
        Demo
        """
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        team.md
            Sprint planning
            Daily standup
          - Two week sprint
          + Three week sprint
            Retrospective
            Demo

        1 note would change.
        """

    Scenario: A line is added
      Given I have a notebook "Ben Notebook" with note "team" holding:
        """
        Sprint planning
        Retrospective
        """
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to:
        """
        Sprint planning
        Daily standup
        Retrospective
        """
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        team.md
            Sprint planning
          + Daily standup
            Retrospective

        1 note would change.
        """

    Scenario: A line is removed
      Given I have a notebook "Ben Notebook" with note "team" holding:
        """
        Sprint planning
        Daily standup
        Retrospective
        """
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to:
        """
        Sprint planning
        Retrospective
        """
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        team.md
            Sprint planning
          - Daily standup
            Retrospective

        1 note would change.
        """

    Scenario: Two changes far apart become two hunks
      Given I have a notebook "Ben Notebook" with note "team" holding:
        """
        Sprint planning
        Two week sprint
        Daily standup
        Backlog refinement
        Story mapping
        Estimation
        Definition of done
        Working agreement
        Team charter
        Retrospective
        Demo
        """
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to:
        """
        Sprint planning
        Three week sprint
        Daily standup
        Backlog refinement
        Story mapping
        Estimation
        Definition of done
        Working agreement
        Team charter
        Retrospective and demo
        Demo
        """
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        team.md
          @@ line 1 @@
            Sprint planning
          - Two week sprint
          + Three week sprint
            Daily standup
            Backlog refinement
            Story mapping
          @@ line 7 @@
            Definition of done
            Working agreement
            Team charter
          - Retrospective
          + Retrospective and demo
            Demo

        1 note would change.
        """

    Scenario: A change at the first line has no context before it
      Given I have a notebook "Ben Notebook" with note "team" holding:
        """
        Sprint planning
        Daily standup
        Retrospective
        """
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to:
        """
        Sprint planning meeting
        Daily standup
        Retrospective
        """
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        team.md
          - Sprint planning
          + Sprint planning meeting
            Daily standup
            Retrospective

        1 note would change.
        """

    Scenario: A change at the last line has no context after it
      Given I have a notebook "Ben Notebook" with note "team" holding:
        """
        Sprint planning
        Daily standup
        Retrospective
        """
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to:
        """
        Sprint planning
        Daily standup
        Retrospective and demo
        """
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        team.md
            Sprint planning
            Daily standup
          - Retrospective
          + Retrospective and demo

        1 note would change.
        """

  Rule: Content is compared as raw text

    Background:
      Given I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: A blank line is part of the content
      Given I have a notebook "Ben Notebook" with note "team" holding:
        """
        Sprint planning

        Retrospective
        """
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "team" is changed in Doughnut to:
        """
        Sprint planning
        Daily standup
        Retrospective
        """
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        team.md
            Sprint planning
          -
          + Daily standup
            Retrospective

        1 note would change.
        """

    Scenario: The content is emptied
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "less" is changed in Doughnut to ""
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md
          - Hello

        1 note would change.
        """

    Scenario: Markdown markup is compared, not rendered
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content                        |
        | less  | **Put** to sleep is _sedation_ |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "less" is changed in Doughnut to "**Put** to sleep is **sedation**"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md
          - **Put** to sleep is _sedation_
          + **Put** to sleep is **sedation**

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
      When I edit "less.md" in the workspace "./BenNotebook" to "Hello from Obsidian"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md
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

    Scenario: The scratch export does not survive the run
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then the scratch directory the preview exported into should no longer exist

    Scenario: Running the preview twice reports the same difference
      When the note "less" is changed in Doughnut to "Hello world!"
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the preview in past CLI assistant messages:
        """
        less.md
          - Hello
          + Hello world!

        1 note would change.
        """

  Rule: Failures are reported instead of a diff

    Background:
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: The workspace path does not exist
      When I enter the slash command "/sync --dry-run ./NoSuchWorkspace" in the interactive CLI
      Then I should see "No directory at ./NoSuchWorkspace." in past CLI assistant messages

    Scenario: The notebook was deleted while the context was open
      When the notebook "Ben Notebook" is deleted in Doughnut
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see "Ben Notebook no longer exists in Doughnut." in past CLI assistant messages

    Scenario: The session expired before the export
      When the access token is no longer valid
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then I should see the session expired message in past CLI assistant messages

    Scenario: A failed export leaves no scratch directory behind
      When the export fails partway
      And I enter the slash command "/sync --dry-run ./BenNotebook" in the interactive CLI
      Then the scratch directory the preview exported into should no longer exist
