@usingMockedOpenAiService
Feature: AI-assisted book layout reorganization

  Scenario: AI reorganize opens preview dialog and applies on confirm
    Given I am logged in as an existing user
    And I have a notebook "Refactoring read" with a note "Code Refactoring Book"
    When I attach a fake blank pdf book with layout of "refactoring" to the notebook "Code Refactoring Book"
    And I open the book attached to notebook "Refactoring read"
    And OpenAI returns a layout suggestion that indents block "2. The Usual Defi nition Is Not Enough" for notebook "Code Refactoring Book"
    When I request AI reorganization of the book layout
    Then I should see a reorganization preview dialog
    And the preview should show block "2. The Usual Defi nition Is Not Enough" with suggested depth 1
    When I confirm the AI suggestion
    Then the book block "2. The Usual Defi nition Is Not Enough" should be at depth 1 in the book layout
