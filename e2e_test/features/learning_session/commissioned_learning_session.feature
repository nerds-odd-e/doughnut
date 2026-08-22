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
    And the learning session request should include focus note with note body "Hello"
    And the learning session request should include related notes with note body "Greetings"
    And the learning session request should instruct the tutor to report a grade and descriptive text per session item
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

      <session_item_grades>
      Hola: 4
      Gracias: 1
      </session_item_grades>
      """
    Then the recorded Feedback for notebook "Spanish conversation" should be shown
    And the commissioned memory tracker for "Hola" should have recall count 1
    And the commissioned memory tracker for "Gracias" should have recall count 1
    And the commissioned memory tracker for "Hola" should have tutor feedback grade 4
    And I should see 0 potential learning session for notebook "Spanish conversation"

  Scenario: Recording a session item feedback report shows tutor Feedback text on the tracker
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And It's day 2, 9 hour
    When I open the learning session request for notebook "Spanish conversation"
    And I record the learning session report:
      """
      # Learning Session Report

      <session_item_feedback>
      ### Hola
      Grade: 4
      Pronunciation was clear; still mixes ser/estar under pressure.

      ### Gracias
      Grade: 1
      Needed several reminders on the soft g.
      </session_item_feedback>
      """
    Then the recorded Feedback for notebook "Spanish conversation" should be shown
    When I visit the commissioned memory tracker for "Hola"
    Then I should see the tutor's feedback "Pronunciation was clear; still mixes ser/estar under pressure."

  Scenario: Request carries the last two dated Feedbacks per Session Item
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And I have recorded a learning session for notebook "Spanish conversation" on day 2, 9 hour with feedback:
      | Note    | Grade | Text                                |
      | Hola    | 3     | Pronunciation was clear             |
      | Gracias | 1     | Needed several reminders on the soft g |
    And I have recorded a learning session for notebook "Spanish conversation" on day 4, 16 hour with feedback:
      | Note    | Grade | Text             |
      | Hola    | 4     | Fluent greeting  |
      | Gracias | 1     | Still needed help |
    And It's day 25, 8 hour
    When I open the learning session request for notebook "Spanish conversation"
    Then the learning session request should include dated Feedbacks for "Hola":
      | Grade | Text                    |
      | 3     | Pronunciation was clear |
      | 4     | Fluent greeting         |

  Scenario Outline: First tutor grade on a new tracker sets Stability and Difficulty
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And I have recorded a learning session for notebook "Spanish conversation" on day 2 with grades:
      | Note    | Grade   |
      | Hola    | <grade> |
      | Gracias | 1       |
    When I visit the commissioned memory tracker for "Hola"
    Then I should see Stability <Stability>
    And I should see Difficulty <Difficulty>
    And I should see <hours> hours between last and next recall

    Examples:
      | grade | Stability | Difficulty | hours |
      | 4     | 199       | 1          | 199   |
      | 3     | 55        | 2.1181     | 55    |
      | 2     | 31        | 5.11217    | 31    |
      | 1     | 5         | 6.4133     | 5     |

  Scenario Outline: On-time second tutor grade grows Stability
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    And I have recorded a learning session for notebook "Spanish conversation" on day 2 with grades:
      | Note    | Grade |
      | Hola    | 3     |
      | Gracias | 1     |
    And It's day 4, 16 hour
    When I open the learning session request for notebook "Spanish conversation"
    And I record the learning session report:
      """
      # Learning Session Report

      <session_item_grades>
      Hola: <grade>
      Gracias: 1
      </session_item_grades>
      """
    And I visit the commissioned memory tracker for "Hola"
    Then I should see Stability <Stability>

    Examples:
      | grade | Stability |
      | 4     | 484       |
      | 3     | 284       |
      | 2     | 193       |
