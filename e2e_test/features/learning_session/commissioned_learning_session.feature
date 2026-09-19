@disableOpenAiService @mockBrowserTime
Feature: Commissioned learning session
  As a learner, I want to assimilate a note as a commissioned memory tracker
  so that a tutor can later conduct a learning session for it.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Spanish conversation" with notes:
      | Title   | Content                    |
      | Saludos | Greetings                  |
      | Hola    | Hello. See [[Saludos]]     |
      | Gracias | Thank you                  |
    And It's day 1, 8 hour

  Scenario: Assimilating a note with a tutor creates a commissioned memory tracker
    Given the note "Hola" was assimilated on day 1
    When I am assimilating the note "Hola"
    And I assimilate it as commissioned
    And I open the assimilation panel
    Then I should see ordinary and commissioned memory trackers for "Hola"

  Scenario: Due commissioned trackers await a Tutor per notebook rather than ordinary recall
    Given I have a notebook "Kanji" with notes:
      | Title | Content |
      | 水    | water   |
    And the notes "Hola, Gracias" in notebook "Spanish conversation" are assimilated as commissioned on day 1
    And the notes "水" are assimilated as commissioned on day 1
    When It's day 2, 9 hour
    Then I should see that I have 0 notes to recall
    And I should see 1 potential learning session for notebook "Spanish conversation"
    And I should see 1 potential learning session for notebook "Kanji"

  Scenario: Opening a potential learning session shows the request without persisting a session
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And It's day 2, 9 hour
    When I open the learning session request for notebook "Spanish conversation"
    Then the learning session request should list session items for notes "Hola, Gracias"
    And the learning session request should include the tutoring status of "Hola"
    And the learning session request should include focus note with note body "Hello"
    And the learning session request should include related notes with note body "Greetings"
    And the learning session request should instruct the tutor to report a grade and descriptive text per session item
    And I should see 1 potential learning session for notebook "Spanish conversation"

  Scenario: Recording the tutor's report closes the session and reschedules each tracker
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And It's day 2, 9 hour
    When I open the learning session request for notebook "Spanish conversation"
    And I record the learning session report:
      """
      # Learning Session Report

      <session_item_feedback>
      <session_item>
      Hola: 4
      Pronunciation was clear; still mixes ser/estar under pressure.
      </session_item>
      <session_item>
      Gracias: 1
      Needed several reminders on the soft g.
      </session_item>
      </session_item_feedback>
      """
    Then the recorded Feedback for notebook "Spanish conversation" should be shown
    And I should see 0 potential learning session for notebook "Spanish conversation"
    And the commissioned memory tracker for "Hola" should have recall count 1
    And the commissioned memory tracker for "Gracias" should have recall count 1
    And the commissioned memory tracker for "Hola" should have tutor feedback grade 4
    When I visit the commissioned memory tracker for "Hola"
    Then I should see the tutor's feedback "Pronunciation was clear; still mixes ser/estar under pressure."
    And I should see Stability 199
    And I should see Difficulty 1
    And I should see 199 hours between last and next recall
