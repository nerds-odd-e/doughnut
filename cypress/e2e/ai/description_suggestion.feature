Feature: Suggest a description based on note title
  I want the AI to suggest a description based on my note title,
  so I can overcome writer's block and generate fresh content quickly.

  Background:
    Given I've logged in as an existing user
    And I have a note with the title "Animals"

  @usingMockedOpenAiService
  Scenario: Generate Description
    Given OpenAI by default returns text completion "Living organism, not plant or fungi."
    When I ask for a description suggestion for note "Animals"
    Then I should see the note description on current page becomes "Living organism, not plant or fungi."

  @usingMockedOpenAiService
  Scenario: Open AI service availability
    Given open AI service always think the system token is invalid
    When I ask for a description suggestion for note "Animals"
    Then I should see that the open AI service is not available in controller bar

  @usingMockedOpenAiService
  Scenario: Suggestions parts are displayed as soon as they are available
    Given OpenAI returns an incomplete text completion "Living organism,"
    When I ask for a description suggestion for note "Animals"
    Then I should see the note description on current page becomes "Living organism,"
    When OpenAI by default returns text completion "not plant or fungi." from now
    Then I should see the note description on current page becomes "Living organism, not plant or fungi."
