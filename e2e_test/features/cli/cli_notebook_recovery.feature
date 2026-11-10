@bundleCliE2eInstall @withCliConfig
Feature: Recover reviewed live edits and publish retained local work
  As a notebook owner, I want recovery and my pending work to reach a clean clone without losing history or learning.

  Scenario: Reviewed live-only content survives recovery, a real rebase conflict and publication
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And I am logged in as an existing user
    And I have a notebook "CLI Recovery Notebook"
    And I have a note "Overview" under notebook "CLI Recovery Notebook" with content:
      """
      ---
      type: Note
      ---
      Original overview
      """
    And the notebook "CLI Recovery Notebook" has readme content "Recovery landing"
    And I retain the learning identity of "Overview" before recovery
    And I capture the note id of "Overview"
    And the notebook "CLI Recovery Notebook"'s Git binding reflects its current content
    And I have a valid Donut Access Token with label "Recovery owner"
    When I clone the notebook "CLI Recovery Notebook" into a temporary destination using the installed CLI
    And I clone the notebook "CLI Recovery Notebook" into a second temporary destination using the installed CLI
    And I commit the following related additions and edit together in the cloned checkout:
      | path        | content                                      |
      | Overview.md | ---\ntype: Note\n---\nLocal overview             |
      | Local A.md  | ---\ntype: Note\n---\nFirst retained addition    |
      | Local B.md  | ---\ntype: Note\n---\nSecond retained addition   |
    Given note "Overview" has content:
      """
      ---
      type: Note
      ---
      Web overview
      """
    And notebook "CLI Recovery Notebook" has an unaccepted live-only note "Recovered" with content:
      """
      ---
      type: Note
      ---
      Genuine live-only authored text
      """
    When I review live-only note "Recovered" in notebook "CLI Recovery Notebook" using the installed CLI
    And I recover the reviewed live edits in notebook "CLI Recovery Notebook" using the installed CLI
    And I pull the cloned checkout expecting rejection from the installed CLI
    Then the cloned checkout has a paused rebase conflict for "Overview.md"
    When I write, stage, and continue the cloned checkout rebase with the following edit to "Overview.md":
      """
      ---
      type: Note
      ---
      Web overview and local overview
      """
    Then the cloned checkout is a clean resolved child of the accepted head
    And the cloned checkout retains the original local commit author and message for "Overview.md"
    When I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the resolved local head as the accepted head
    When I pull the second cloned checkout using the installed CLI
    Then the second cloned checkout retains its original head as an ancestor
    And the second cloned checkout is a clean checkout of the notebook "CLI Recovery Notebook" accepted head
    And the second cloned checkout contains exactly:
      | README.md    |
      | Overview.md  |
      | Recovered.md |
      | Local A.md   |
      | Local B.md   |
    And the second cloned checkout file "Recovered.md" is:
      """
      ---
      type: Note
      ---
      Genuine live-only authored text
      """
    And the second cloned checkout file "Overview.md" is:
      """
      ---
      type: Note
      ---
      Web overview and local overview

      """
    And the second cloned checkout file "Local A.md" is:
      """
      ---
      type: Note
      ---
      First retained addition

      """
    And the second cloned checkout file "Local B.md" is:
      """
      ---
      type: Note
      ---
      Second retained addition

      """
    And the original note in Donut should be "CLI Recovery Notebook/Overview" with content:
      """
      Web overview and local overview
      """
    And the learning identity of "Overview" is retained after recovery and publication
