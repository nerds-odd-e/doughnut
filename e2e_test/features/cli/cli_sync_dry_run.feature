@ignore
# Specified at the refinement session on 2026-07-27; see
# docs/refinement/2026-07-27/SPEC-sync-dry-run.md.
# The command and its diffing are covered by the CLI unit tests and work end to
# end; these scenarios are not enabled yet because their step definitions have
# not been run green. Enable them in the commit that does.
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

    Background:
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      And I enter the slash command "/use Ben Notebook" in the interactive CLI

    Scenario: Preview one changed note
      When the note "less" is changed in Doughnut to "Hello world!"
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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

    # Asserting the absence of text needs a negative assertion the CLI
    # transcript helpers do not offer yet. The unit tests cover it.
    @wip
    Scenario: An unchanged note is not reported
      Given I have a notebook "Ben Notebook" with notes:
        | Title | Content |
        | less  | Hello   |
        | scrum | Sprint  |
      And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
      When the note "less" is changed in Doughnut to "Hello world!"
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
      Then I should see the preview in past CLI assistant messages:
        """
        less.md
          - Hello from Obsidian
          + Hello

        1 note would change.
        """

    Scenario: No difference to report
      When I preview the pull into the workspace "./BenNotebook"
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
      And I preview the pull into the workspace "./BenNotebook"
      Then the file "less.md" in the workspace "./BenNotebook" should hold "Hello"

    Scenario: The preview adds no files of its own
      When the note "less" is changed in Doughnut to "Hello world!"
      And I preview the pull into the workspace "./BenNotebook"
      Then the workspace "./BenNotebook" should hold only:
        | Path    |
        | less.md |

    Scenario: Running the preview twice reports the same difference
      When the note "less" is changed in Doughnut to "Hello world!"
      And I preview the pull into the workspace "./BenNotebook"
      And I preview the pull into the workspace "./BenNotebook"
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
      When I preview the pull into the workspace "./NoSuchWorkspace"
      Then I should see "No directory at ./NoSuchWorkspace." in past CLI assistant messages

    # Deleting a notebook needs a testability endpoint that does not exist yet.
    @wip
    Scenario: The notebook was deleted while the context was open
      When the notebook "Ben Notebook" is deleted in Doughnut
      And I preview the pull into the workspace "./BenNotebook"
      Then I should see "Ben Notebook no longer exists in Doughnut." in past CLI assistant messages

    # Invalidating a token mid-session needs testability support that does not
    # exist yet.
    @wip
    Scenario: The session expired before the export
      When the access token is no longer valid
      And I preview the pull into the workspace "./BenNotebook"
      Then I should see "Access token is invalid or expired." in past CLI assistant messages

    Scenario: Pulling is turned away rather than assumed
      When I run sync without --dry-run on the workspace "./BenNotebook"
      Then I should see "Only /sync --dry-run is available." in past CLI assistant messages
