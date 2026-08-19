@disableOpenAiService @mockBrowserTime
Feature: Commissioned learning session
  As a learner, I want to assimilate a note as a commissioned memory tracker
  so that a tutor can later conduct a learning session for it.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Spanish conversation" with notes:
      | Title   | Content   |
      | Hola    | Hello     |
      | Gracias | Thank you |
    And It's day 1, 8 hour

  Scenario: Assimilating a note with a tutor creates a commissioned memory tracker
    Given the note "Hola" was assimilated on day 1
    When I am assimilating the note "Hola"
    And I assimilate it as commissioned
    And I open assimilation settings
    Then I should see ordinary and commissioned memory trackers for "Hola"

  Scenario: Due commissioned trackers await a Tutor rather than ordinary recall
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    When It's day 2, 9 hour
    Then I should see that I have 0 notes to recall
    And I should see 1 potential learning session for notebook "Spanish conversation"

  Scenario: Opening a potential learning session shows the request without persisting a session
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And It's day 2, 9 hour
    When I open the learning session request for notebook "Spanish conversation"
    Then the learning session request should list session items for notes "Hola, Gracias"
    And the learning session request should include the tutoring status of "Hola"
    And the learning session request should include focus context with note body "Hello"
    And the learning session request should instruct the tutor to report one score per session item
    And I should see 1 potential learning session for notebook "Spanish conversation"

  Scenario: Notes from different notebooks are commissioned as separate learning sessions
    Given I have a notebook "Kanji" with notes:
      | Title | Content |
      | 水    | water   |
    And the notes "Hola, Gracias" in notebook "Spanish conversation" are assimilated as commissioned on day 1
    And the notes "水" are assimilated as commissioned on day 1
    When It's day 2, 9 hour
    Then I should see that I have 0 notes to recall
    And I should see 1 potential learning session for notebook "Spanish conversation"
    And I should see 1 potential learning session for notebook "Kanji"

  Scenario: Recording the tutor's report writes Feedback and schedules each tracker
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And It's day 2, 9 hour
    When I open the learning session request for notebook "Spanish conversation"
    And I record the learning session report:
      """
      # Learning Session Report

      <session_item_scores>
      Hola: 4
      Gracias: 1
      </session_item_scores>
      """
    Then the recorded Feedback for notebook "Spanish conversation" should be shown
    And the commissioned memory tracker for "Hola" should have recall count 1
    And the commissioned memory tracker for "Gracias" should have recall count 1
    And the commissioned memory tracker for "Hola" should have tutor feedback score 4
    And I should see 0 potential learning session for notebook "Spanish conversation"

  Scenario Outline: First tutor score on a new tracker sets Stability and Difficulty
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And I have recorded a learning session for notebook "Spanish conversation" on day 2 with scores:
      | Note    | Score   |
      | Hola    | <score> |
      | Gracias | 1       |
    When I visit the commissioned memory tracker for "Hola"
    Then I should see Stability <Stability>
    And I should see Difficulty <Difficulty>
    And I should see <hours> hours between last and next recall

    Examples:
      | score | Stability | Difficulty | hours |
      | 4     | 199       | 1          | 199   |
      | 3     | 55        | 2.1181     | 55    |
      | 2     | 31        | 5.11217    | 31    |
      | 1     | 5         | 6.4133     | 5     |

  Scenario: Recording tutor score 3 leaves a GOOD RecallLog
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And I have recorded a learning session for notebook "Spanish conversation" on day 2 with scores:
      | Note    | Score |
      | Hola    | 3     |
      | Gracias | 1     |
    When I visit the commissioned memory tracker for "Hola"
    Then I should see a GOOD RecallLog with elapsed hours and no answer id

  Scenario Outline: On-time second tutor score grows Stability
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And I have recorded a learning session for notebook "Spanish conversation" on day 2 with scores:
      | Note    | Score |
      | Hola    | 3     |
      | Gracias | 1     |
    And It's day 4, 16 hour
    When I open the learning session request for notebook "Spanish conversation"
    And I record the learning session report:
      """
      # Learning Session Report

      <session_item_scores>
      Hola: <score>
      Gracias: 1
      </session_item_scores>
      """
    And I visit the commissioned memory tracker for "Hola"
    Then I should see Stability <Stability>

    Examples:
      | score | Stability |
      | 4     | 484       |
      | 3     | 284       |
      | 2     | 193       |
