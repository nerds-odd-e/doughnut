@withCliConfig
@interactiveCLI
Feature: Check whether a workspace follows the OKF format

  As a workspace owner
  I want to lint my local workspace against OKF
  So that I know the bundle can be consumed by OKF-oriented, Obsidian and ordinary Markdown tools

  Background:
    Given an empty workspace "./Workspace"

  Scenario: A concept without frontmatter is an error
    Given the workspace "./Workspace" has a file "a.md" with content:
      """
      # apple
      """
    When I enter the slash command "/lint ./Workspace" in the interactive CLI
    Then I should see "a.md:1 error Frontmatter is missing" with any spacing in past CLI assistant messages
    And I should see "1 error in 1 file." in past CLI assistant messages

  @ignore
  Scenario: A file that is not a concept is a warning, not an error
    Given the workspace "./Workspace" has a file "a.md" with content:
      """
      ---
      type: concept
      ---

      # apple
      """
    And the workspace "./Workspace" has a file "a.json" with content:
      """
      {}
      """
    When I enter the slash command "/lint ./Workspace" in the interactive CLI
    Then I should see "a.json warning Not an OKF concept" with any spacing in past CLI assistant messages
    And I should see "Workspace follows the OKF format." in past CLI assistant messages
    And I should see "1 warning in 1 file." in past CLI assistant messages

  # A clean report is what proves the dot folder was not walked and the link in
  # the body was not followed; either would raise an error otherwise.
  @ignore
  Scenario: A conformant bundle reports nothing
    Given the workspace "./Workspace" has a file "a.md" with content:
      """
      ---
      type: concept
      ---

      # apple

      See [banana](./banana.md)
      """
    And the workspace "./Workspace" has a file ".git/config.md" with content:
      """
      no frontmatter here
      """
    When I enter the slash command "/lint ./Workspace" in the interactive CLI
    Then I should see "Workspace follows the OKF format." in past CLI assistant messages

  # The run does not stop at the first problem: `a.md` has two (no `type` key, and
  # `tags` is not a list), `b.md` has one. The counts are what prove nothing was
  # dropped; the layout of each line is unit-tested.
  Scenario: Every problem in the bundle is reported in one run
    Given the workspace "./Workspace" has a file "a.md" with content:
      """
      ---
      tags: not-a-list
      ---

      # apple
      """
    And the workspace "./Workspace" has a file "b.md" with content:
      """
      # banana
      """
    When I enter the slash command "/lint ./Workspace" in the interactive CLI
    Then I should see "a.md" in past CLI assistant messages
    And I should see "b.md" in past CLI assistant messages
    And I should see "2 errors, 1 warning in 2 files." in past CLI assistant messages
