@mockBrowserTime @usingMockedOpenAiService
Feature: Export the highlighted property
  Scenario: Share the second duplicate's context and existing quiz evidence
    Given I am logged in as an existing user
    And I have a notebook "Export focus"
    And I have a note "run" under notebook "Export focus" with content:
      """
      To move quickly on foot.
      """
    And I have a note "walk" under notebook "Export focus" with content:
      """
      To move at a walking pace.
      """
    And I have a note "Activities" under notebook "Export focus" with content:
      """
      ---
      example of: ["[[run]]", "[[walk]]"]
      ---
      Surrounding activity explanation.
      """
    And It's day 1, 8 hour
    When I visit note "Activities"
    And I assimilate the value "[[run]]" of property "example of"
    Given OpenAI generates this question:
      | Question Stem          | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | Which activity is fast? | run           | walk               | swim               | jump               |
    And OpenAI evaluates the question as legitimate
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "Which activity is fast?"
    When I choose answer "walk"
    And I update note "Activities" content using markdown to become:
      """
      ---
      example of: ["[[run]]", "[[walk]]", "[[run]]"]
      ---
      Surrounding activity explanation.
      """
    And I open occurrence 2 of item "[[run]]" in property "example of" of note "Activities" from its URL
    Then occurrence 2 of item "[[run]]" in property "example of" should be highlighted
    When I export the highlighted note context
    Then the note export should contain "Highlighted item occurrence: 2"
    And the note export should contain "Surrounding activity explanation."
    When I include recent questions in the note export
    Then the note export should contain "Which activity is fast?"
    And the note export should contain "Donut answer key: run"
    And the note export should contain "Learner's selected answer: walk"
    And the note export should contain "Quiz history is shared"
