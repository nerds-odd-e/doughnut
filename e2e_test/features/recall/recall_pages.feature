Feature: Review Pages
  As a learner, I want to review my notes and links so that I have fresh memory.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Title    | Details         | Image Url   | Parent Title |
      | Sedition | Incite violence |             | English      |
      | Sedation | Put to sleep    |             | English      |
      | Sedative | Sleep medicine  | a_slide.jpg | English      |
    And there is "similar to" relationship between note "Sedition" and "Sedation"

  Scenario: Different review pages for different notes
    * I assimilate these in sequence:
      | Review Type  | Title    | Additional Info             |
      | single note  | Sedition | Incite violence             |
      | single note  | Sedation | Put to sleep                |
      | image note   | Sedative | Sleep medicine; a_slide.jpg |
      | link         | Sedition | similar to; Sedation        |
      | assimilation done |          |                             |

  Scenario: Count of recall and assimilate notes
    Given It's day 1, 8 hour
    And I assimilate these in sequence:
      | Review Type | Title    |
      | single note | Sedition |
    When It's day 2, 9 hour
    Then I should see that I have 1 notes to recall
    And I should see that I have 3 new notes to assimilate

