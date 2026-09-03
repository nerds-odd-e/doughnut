@mockBrowserTime
Feature: Property memory tracker
  As a learner, I want to assimilate a single note property for recall
  so that I can practice that property independently of the whole note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Property recall"
    And I have a note "Vitamins" under notebook "Property recall" with content:
      """
      ---
      topic: micronutrients
      ---

      Vitamin notes body.
      """
    And It's day 1, 8 hour
    And the note "Vitamins" has assimilated property "topic"

  @usingMockedOpenAiService
  Scenario: Answering a property recall question updates only the property tracker
    Given I am viewing the assimilation panel for note "Vitamins"
    Then I should see a property memory tracker for "topic"
    When It's day 2, 9 hour
    Then I should see that I have 1 notes to recall
    Given It's day 1, 20 hour
    And I assimilated one note "Vitamins" at the current time
    And OpenAI generates this question:
      | Question Stem                      | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What does the topic property mean? | micronutrients | vitamins           | minerals           | proteins           |
    And OpenAI evaluates the question as legitimate
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "What does the topic property mean?"
    When I choose answer "micronutrients"
    And I visit note "Vitamins"
    And I open the assimilation panel
    Then the note memory tracker should have recall count 0
    And the property memory tracker for "topic" should have recall count 1

  @usingMockedOpenAiService
  Scenario: Following the note from a property recall answer opens that property
    And OpenAI generates this question:
      | Question Stem                      | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What does the topic property mean? | micronutrients | vitamins           | minerals           | proteins           |
    And OpenAI evaluates the question as legitimate
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "What does the topic property mean?"
    When I choose answer "vitamins"
    Then I should see that my MCQ answer "vitamins" is incorrect
    When I follow the note under question "Vitamins"
    Then I should be at property "topic" of note "Vitamins"
    And the rich note property "topic" should be focused with its property panel open
