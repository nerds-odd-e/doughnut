@withCliConfig
Feature: CLI input box display

  @interactiveCLI
  Scenario: Enter on empty input does not show duplicate top border
    When I press Enter in the interactive CLI
    Then the input box UI should be normal
