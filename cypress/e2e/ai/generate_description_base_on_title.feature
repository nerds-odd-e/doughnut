Feature: Generate a description based on note title

I want the AI to generate a description based on my note title, 
so I can overcome writer's block and generate fresh content quickly.

  Background:
    Given I've logged in as an existing user
    And I have a note with the title "Animals"

    @usingMockedOpenAiService
    Scenario: Generate Description
      Given OpenAI always return text completion "are all livings"
      When I generate description from "Animals"
      Then I should see the note description on current page becomes "are all livings"

    @usingMockedOpenAiService
    Scenario: Open AI service availability
      Given open AI service always think the system token is invalid
      When I generate description from "Animals"
      Then I should see that the open AI service is not available in controller bar
