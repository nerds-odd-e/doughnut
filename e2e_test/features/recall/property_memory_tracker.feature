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

  @disableOpenAiService @wip
  Scenario: Untracked example of property appears in assimilation queue
    Given I am re-logged in as "another_old_learner"
    And I have a notebook "Property queue"
    And I have a note "Kanji" under notebook "Property queue" with content:
      """
      ---
      example of: "[[Sentence]]"
      ---

      Body.
      """
    And It's day 1, 8 hour
    And I assimilated one note "Kanji" at the current time
    When I start assimilation from the menu
    Then I should see assimilation progress "1/2/2"
    And I should see pending assimilation property "example of"

  @disableOpenAiService @wip
  Scenario: Skip a property does not create a dummy understanding tracker
    Given I am re-logged in as "another_old_learner"
    And I have a notebook "Property skip"
    And I have a note "Minerals" under notebook "Property skip" with content:
      """
      ---
      topic: calcium
      ---

      Body.
      """
    And It's day 1, 8 hour
    And I assimilated one note "Minerals" at the current time
    When I start assimilation from the menu
    Then I should see pending assimilation property "topic"
    When I skip rich note property "topic" from its toggle options
    Then I should see the no more notes to assimilate toast
    When I visit note "Minerals"
    And I open assimilation settings
    Then the property memory tracker for "topic" should be absent
    And assimilate for property "topic" should be enabled
    And I should see Return to sequence for property "topic"

  @disableOpenAiService @wip
  Scenario: Return to sequence restores a skipped property to the sequence
    Given I am re-logged in as "another_old_learner"
    And I have a notebook "Property return"
    And I have a note "Minerals" under notebook "Property return" with content:
      """
      ---
      topic: calcium
      ---

      Body.
      """
    And It's day 1, 8 hour
    And I assimilated one note "Minerals" at the current time
    When I start assimilation from the menu
    Then I should see pending assimilation property "topic"
    When I skip rich note property "topic" from its toggle options
    Then I should see the no more notes to assimilate toast
    When I visit note "Minerals"
    And I open assimilation settings
    Then I should see Return to sequence for property "topic"
    When I return rich note property "topic" to the sequence from its toggle options
    Then I should see Skip for property "topic"
    When I start assimilation from the menu
    Then I should see pending assimilation property "topic"

  @disableOpenAiService @wip
  Scenario: Assimilating a skipped property creates a property understanding tracker
    Given I am re-logged in as "another_old_learner"
    And I have a notebook "Property skip assimilate"
    And I have a note "Minerals" under notebook "Property skip assimilate" with content:
      """
      ---
      topic: calcium
      ---

      Body.
      """
    And It's day 1, 8 hour
    And I assimilated one note "Minerals" at the current time
    When I start assimilation from the menu
    Then I should see pending assimilation property "topic"
    When I skip rich note property "topic" from its toggle options
    Then I should see the no more notes to assimilate toast
    When I visit note "Minerals"
    And I open assimilation settings
    And I assimilate rich note property "topic" from its toggle options
    Then I should see the no more notes to assimilate toast
    When I visit note "Minerals"
    And I open assimilation settings
    Then I should see a property memory tracker for "topic"

  @disableOpenAiService
  Scenario: Remove from recall on assimilation settings for a property
    Given I assimilated one note "Vitamins" at the current time
    And I am viewing assimilation settings for note "Vitamins"
    Then I should see Remove from recall for property "topic"
    When I remove rich note property "topic" from recall from its toggle options
    Then I should see Revive for property "topic"
    When I start assimilation from the menu
    Then I should see the no more notes to assimilate toast

  @disableOpenAiService
  Scenario: Note-level assimilation stays available after property-only assimilation
    Given I am viewing assimilation settings for note "Vitamins"
    Then assimilate for property "topic" should be disabled
    And assimilate should be enabled
    When I assimilate on the assimilation panel
    And I open assimilation settings
    Then the note memory tracker should have recall count 0
    And I should see a property memory tracker for "topic"

  @disableOpenAiService
  Scenario: Assimilated property appears as a labeled tracker and becomes due for recall
    Given I am viewing assimilation settings for note "Vitamins"
    Then I should see a property memory tracker for "topic"
    When It's day 2, 9 hour
    Then I should see that I have 1 notes to recall

  @usingMockedOpenAiService
  Scenario: Answering a property recall question updates only the property tracker
    And It's day 1, 20 hour
    And I assimilated one note "Vitamins" at the current time
    And OpenAI generates this question:
      | Question Stem                      | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What does the topic property mean? | micronutrients | vitamins           | minerals           | proteins           |
    And OpenAI evaluates the question as legitimate
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "What does the topic property mean?"
    When I choose answer "micronutrients"
    And I visit note "Vitamins"
    And I open assimilation settings
    Then the note memory tracker should have recall count 0
    And the property memory tracker for "topic" should have recall count 1

  @usingMockedOpenAiService
  Scenario: Recalling a property tracker sends property focus to OpenAI
    And OpenAI generates this question:
      | Question Stem                      | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What does the topic property mean? | micronutrients | vitamins           | minerals           | proteins           |
    And OpenAI evaluates the question as legitimate
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "What does the topic property mean?"
    And OpenAI Responses POST bodies include property focus for "topic" with value "micronutrients"

  Scenario: Removing tracked property deletes property memory tracker
    When I visit note "Vitamins"
    And I remove rich note property "topic" confirming memory tracker change
    And I open assimilation settings
    Then the property memory tracker for "topic" should be absent

  @disableOpenAiService
  Scenario: Assimilate a property from its own toggle-options row
    Given I am re-logged in as "another_old_learner"
    And I have a notebook "Property toggle"
    And I have a note "Iron" under notebook "Property toggle" with content:
      """
      ---
      topic: iron
      ---

      Body.
      """
    And It's day 1, 8 hour
    And I assimilated one note "Iron" at the current time
    When I visit note "Iron"
    And I assimilate rich note property "topic" from its toggle options
    Then I should see the no more notes to assimilate toast
    When I visit note "Iron"
    And I open assimilation settings
    Then I should see a property memory tracker for "topic"

  Scenario: Property memory tracker page shows note and focused property
    Given I am viewing assimilation settings for note "Vitamins"
    When I open the property memory tracker for "topic"
    Then I should see note "Vitamins" on the memory tracker page
    And I should see focused property "topic" on the memory tracker page
