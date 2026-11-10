@bundleCliE2eInstall @withCliConfig
Feature: Publish small edits from a large accepted notebook
  As a notebook owner, I want my committed edits accepted and received without a notebook-size limit.

  Scenario: Three small Markdown edits above a one MiB accepted listing reach a fresh clone
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And I am logged in as an existing user
    And I have a notebook "CLI Large Notebook"
    And notebook "CLI Large Notebook" has a large accepted Markdown tree
    And I have a valid Donut Access Token with label "Large notebook owner"
    When I clone the notebook "CLI Large Notebook" into a temporary destination using the installed CLI
    Then notebook "CLI Large Notebook" is healthy with an accepted listing above one MiB
    When I commit three small edits in the large notebook checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I clone the notebook "CLI Large Notebook" into a fresh temporary destination using the installed CLI
    Then the fresh clone retains the large notebook "CLI Large Notebook" history and three edits
