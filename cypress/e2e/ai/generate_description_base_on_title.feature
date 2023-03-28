Feature: I want the AI to generate a description based on my note title, so I can overcome writer's block and generate fresh content quickly.

  Background:
    Given I've logged in as an existing user
    And I have a note with the title "Animals"

    @usingMockedOpenAiService
    @ignore
    Scenario: Generate Description
      Given OpenAI always return text completion "are all livings"
      When I generate description from "Animals"
      Then I can see "are all livings" in description

    @usingMockedOpenAiService
    @ignore
    Scenario: Generate Description Again
      Given I have a description "What goes on"
      When I remove "What goes on" in description
      And I click the robot button
      And OpenAI always return text completion "are all livings"
      Then I can see "are all livings" in description
