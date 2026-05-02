Feature: Assimilation and Recall Pages
  As a learner, I want to assimilate and recall my notes and links so that I have fresh memory.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Details         | Image Url   | Skip Memory Tracking |
      | English  |                 |             | true                   |
      | Sedition | Incite violence |             |                        |
      | Sedation | Put to sleep    |             |                        |
      | Sedative | Sleep medicine  | a_slide.jpg |                        |
    And there is "similar to" relationship between note "Sedition" and "Sedation"

  Scenario: Different assimilation pages for different notes
    * I assimilate these in sequence:
      | Assimilation Type | Title    | Additional Info             |
      | single note       | Sedition | Incite violence             |
      | single note       | Sedation | Put to sleep                |
      | image note        | Sedative | Sleep medicine; a_slide.jpg |
      | link              | Sedition | similar to; Sedation        |
      | assimilation done |          |                             |

  Scenario: Count of recall and assimilate notes
    Given It's day 1, 8 hour
    And I assimilate these in sequence:
      | Assimilation Type | Title    |
      | single note       | Sedition |
    When It's day 2, 9 hour
    Then I should see that I have 1 notes to recall
    And I should see 3 due for assimilation
