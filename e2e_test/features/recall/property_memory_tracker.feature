@mockBrowserTime
Feature: Property memory tracker
  As a learner, I want to assimilate a single note property for recall
  so that I can practice that property independently of the whole note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Property recall" with a note "Vitamins"
    And I update note "Vitamins" content using markdown to become:
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

  @disableOpenAiService
  Scenario: Note-level assimilation stays available after property-only assimilation
    Then the keep for recall button should be enabled
    When I keep for recall on the assimilation panel
    And I open assimilation settings from more options
    Then the note memory tracker should have recall count 0
    And I should see a property memory tracker for "topic" on the assimilation settings panel

  @disableOpenAiService
  Scenario: Assimilating a property shows a labeled tracker and recall item
    Then I should see a property memory tracker for "topic" on the assimilation settings panel
    When It's day 2, 9 hour
    Then I should see that I have 1 notes to recall

  @usingMockedOpenAiService
  Scenario: Answering a property recall question updates only the property tracker
    And It's day 1, 20 hour
    And I assimilated one note "Vitamins" at the current time
    And OpenAI generates this question:
      | Question Stem                         | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What does the topic property mean?    | micronutrients | vitamins           | minerals           |
    And OpenAI evaluates the question as legitimate
    When I am recalling my note on day 2
    Then I should be asked "What does the topic property mean?"
    When I choose answer "micronutrients"
    And I visit note "Vitamins"
    And I open assimilation settings from more options
    Then the note memory tracker should have recall count 0
    And the property memory tracker for "topic" should have recall count 1

  @usingMockedOpenAiService
  Scenario: Recalling a property tracker sends property focus to OpenAI
    And OpenAI generates this question:
      | Question Stem                         | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What does the topic property mean?    | micronutrients | vitamins           | minerals           |
    And OpenAI evaluates the question as legitimate
    When I am recalling my note on day 2
    Then I should be asked "What does the topic property mean?"
    And OpenAI Responses POST bodies include property focus for "topic" with value "micronutrients"

  Scenario: Removing tracked property deletes property memory tracker
    Then I should see a property memory tracker for "topic" on the assimilation settings panel
    When I remove rich note property "topic" confirming memory tracker change
    And I reload the current page for note "Vitamins"
    And I open assimilation settings from more options
    Then the property memory tracker for "topic" should be absent on the assimilation settings panel

  Scenario: Renaming tracked property key updates property memory tracker
    Then I should see a property memory tracker for "topic" on the assimilation settings panel
    When I visit note "Vitamins"
    And I rename rich note property key from "topic" to "subject" confirming memory tracker change
    And I reload the current page for note "Vitamins"
    And I open assimilation settings from more options
    Then I should see a property memory tracker for "subject" on the assimilation settings panel
    And the property memory tracker for "topic" should be absent on the assimilation settings panel

  Scenario: Removing tracked property in markdown mode deletes property memory tracker
    Then I should see a property memory tracker for "topic" on the assimilation settings panel
    When I visit note "Vitamins"
    And I remove markdown note property "topic" confirming memory tracker change
    And I reload the current page for note "Vitamins"
    And I open assimilation settings from more options
    Then the property memory tracker for "topic" should be absent on the assimilation settings panel

  Scenario: Property memory tracker page shows note and focused property
    When I open the property memory tracker for "topic" from the assimilation settings panel
    Then I should see note "Vitamins" on the memory tracker page
    And I should see focused property "topic" on the memory tracker page
