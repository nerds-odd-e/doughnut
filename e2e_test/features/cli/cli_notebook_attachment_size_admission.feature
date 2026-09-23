@bundleCliE2eInstall @withCliConfig
Feature: CLI notebook attachment size admission
  As a notebook owner, I want an oversized attachment publish refused with recovery
  advice so I can amend unpublished history and republish the corrected bytes.

  Background:
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And I am logged in as an existing user
    And I have a notebook "CLI Clone Notebook"
    And I have a note "Overview" under notebook "CLI Clone Notebook" with content:
      """
      ---
      type: Note
      ---
      """
    And the notebook "CLI Clone Notebook" has readme content "Notebook landing"
    And the notebook "CLI Clone Notebook" has a readme-only folder "Recipes" with readme "Folder landing"
    And I have a note "Pasta" under notebook "CLI Clone Notebook" in folder "Recipes" with content:
      """
      ---
      type: Note
      author: Chef Boyardee
      ---
      Boil water
      """
    And the notebook "CLI Clone Notebook" has a readme-only folder "Kitchen" with readme "Kitchen landing"
    And the notebook "CLI Clone Notebook" uses legacy raw Git attachment storage
    And the notebook "CLI Clone Notebook"'s Git binding reflects its current content
    And I have a valid Donut Access Token with label "E2E CLI Clone Token"

  Scenario: Publishing an oversized attachment recovers by amending unpublished history
    When I clone the notebook "CLI Clone Notebook" into a temporary destination using the installed CLI
    And I commit the attachment "oversized.bin" filled with 10485761 bytes of "0x41" in the cloned checkout
    And I publish the cloned checkout expecting rejection from the installed CLI
    Then I should see "Attachment \"oversized.bin\" is 10485761 bytes, which exceeds the 10485760-byte limit" in the non-interactive output
    And I should see "Amend or rebase the unpublished proposal" in the non-interactive output
    And I should see "Do not rewrite already accepted commits" in the non-interactive output
    And I should see "a later tip deletion alone does not clear" in the non-interactive output
    And the cloned checkout retains the original committed proposal
    And the notebook "CLI Clone Notebook" accepted head remains the parent of the cloned checkout
    When I amend the unpublished commit replacing "oversized.bin" with the bytes "89 FF FE 00" in the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I clone the notebook "CLI Clone Notebook" into a fresh temporary destination using the installed CLI
    Then the fresh clone contains exactly:
      | README.md          |
      | Overview.md        |
      | Kitchen/README.md  |
      | Recipes/README.md  |
      | Recipes/Pasta.md   |
      | oversized.bin      |
    And the fresh clone is a clean checkout of the accepted head
    And the fresh clone file "oversized.bin" holds the bytes "89 FF FE 00"
