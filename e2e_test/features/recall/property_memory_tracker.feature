@mockBrowserTime
Feature: Property memory tracker
  As a learner, I want to assimilate a single note property for recall
  so that I can practice that property independently of the whole note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Property recall" with a note "Vitamins"

  @disableOpenAiService
  Scenario: Assimilating a property shows a labeled tracker and recall item
    When I update note "Vitamins" content using markdown to become:
      """
      ---
      topic: micronutrients
      ---

      Vitamin notes body.
      """
    And It's day 1, 8 hour
    And I am assimilating the note "Vitamins"
    And I expand assimilation properties on the assimilation settings panel
    And I assimilate the property "topic" on the assimilation settings panel
    Then I should see a property memory tracker for "topic" on the assimilation settings panel
    When It's day 2, 9 hour
    Then I should see that I have 1 notes to recall

  @usingMockedOpenAiService
  Scenario: Recalling a property tracker sends property focus to OpenAI
    When I update note "Vitamins" content using markdown to become:
      """
      ---
      topic: micronutrients
      ---

      Vitamin notes body.
      """
    And It's day 1, 8 hour
    And I am assimilating the note "Vitamins"
    And I expand assimilation properties on the assimilation settings panel
    And I assimilate the property "topic" on the assimilation settings panel
    And OpenAI generates this question:
      | Question Stem                         | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What does the topic property mean?    | micronutrients | vitamins           | minerals           |
    And OpenAI evaluates the question as legitimate
    When I am recalling my note on day 2
    Then I should be asked "What does the topic property mean?"
    And OpenAI Responses POST bodies include property focus for "topic" with value "micronutrients"
