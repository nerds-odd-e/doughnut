@cli
Feature: CLI install and run
  As a user, I want to install the Doughnut CLI and run it.

  Scenario: Install from localhost and run
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut command
    Then I should see "Hello World"
