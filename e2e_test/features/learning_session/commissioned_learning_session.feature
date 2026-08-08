@disableOpenAiService @mockBrowserTime
Feature: Commissioned learning session
  As a learner, I want to assimilate a note as a commissioned memory tracker
  so that a tutor can later conduct a learning session for it.

  Background:
    Given I am logged in as an existing user
    And my space setting is "1, 2, 4, 8"
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
    And I should see 1 potential learning session to commission for notebook "Spanish conversation"

  Scenario: Commissioning a learning session produces a request for the tutor
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And It's day 2, 9 hour
    When I commission a learning session for notebook "Spanish conversation"
    Then the learning session request should list session items for notes "Hola, Gracias"
    And the learning session request should include the learning status of "Hola"
    And the learning session request should include the expected learning content "Hello"
    And the learning session request should instruct the tutor to report one score per session item
    And the learning session should be awaiting the tutor's report

  Scenario: Notes from different notebooks are commissioned as separate learning sessions
    Given I have a notebook "Kanji" with notes:
      | Title | Content |
      | 水    | water   |
    And the notes "Hola, Gracias" in notebook "Spanish conversation" are assimilated as commissioned on day 1
    And the notes "水" are assimilated as commissioned on day 1
    When It's day 2, 9 hour
    Then I should see that I have 0 notes to recall
    And I should see 1 potential learning session to commission for notebook "Spanish conversation"
    And I should see 1 potential learning session to commission for notebook "Kanji"

  Scenario: Recording the tutor's report schedules each tracker from its score
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And I have commissioned a learning session for notebook "Spanish conversation" on day 2 with session items for notes "Hola, Gracias"
    When I record the learning session report for the learning session of notebook "Spanish conversation":
      """
      # Learning Session Report

      <session_item_scores>
      Hola: 5
      Gracias: 1
      </session_item_scores>
      """
    Then the learning session for notebook "Spanish conversation" should be marked as recorded
    And the commissioned memory tracker for "Hola" should have recall count 1
    And the commissioned memory tracker for "Gracias" should have recall count 1
    And I should see tutor feedback score 5 from a learning session for the memory tracker of note "Hola"
    When It's day 3, 9 hour
    And I commission a learning session for notebook "Spanish conversation"
    Then the learning session request should list session items for only notes "Gracias"

  Scenario: A later report amends the feedback of a recorded learning session
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And I have recorded a learning session for notebook "Spanish conversation" on day 2 with scores:
      | Note    | Score |
      | Hola    | 4     |
      | Gracias | 1     |
    When I record the learning session report for the learning session of notebook "Spanish conversation":
      """
      # Learning Session Report

      <session_item_scores>
      Gracias: 4
      </session_item_scores>
      """
    Then I should see tutor feedback score 4 from a learning session for the memory tracker of note "Gracias"
    When It's day 3, 9 hour
    Then I should see 0 potential learning session to commission for notebook "Spanish conversation"
