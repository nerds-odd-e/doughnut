@withCliConfig
Feature: CLI input box display

  @interactiveCLI
  Scenario: Enter on empty input does not show duplicate top border
    When I press Enter in the interactive CLI
    Then I should see exactly one input box top border
