@usingMockedOpenAiService
Feature: AI-assisted book layout reorganization

  Scenario: AI reorganize opens preview dialog
    Given I am logged in as an existing user
    And I have a notebook with the head note "Code Refactoring Book"
    When I attach a fake blank pdf book with layout of "refactoring" to the notebook "Code Refactoring Book"
    And I open the book attached to notebook "Code Refactoring Book"
    And OpenAI returns a layout suggestion that indents block "2. The Usual Defi nition Is Not Enough" for notebook "Code Refactoring Book"
    When I request AI reorganization of the book layout
    Then I should see a reorganization preview dialog
    And the preview should show block "2. The Usual Defi nition Is Not Enough" with suggested depth 1
