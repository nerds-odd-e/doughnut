@ignore
@withCliConfig
Feature: CLI input box display

  @interactiveCLI
  Scenario: After /clear, Enter on empty input does not show duplicate top border
    When I input "/clear" in the interactive CLI
    And I press Enter in the interactive CLI
    And I press Enter in the interactive CLI
    Then the input box UI should be normal
