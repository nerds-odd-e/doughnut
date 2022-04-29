Feature: Review Pages
  As a learner, I want to review my notes and links so that I have fresh memory.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description     | pictureUrl  |
      | Sedition | Incite violence |             |
      | Sedation | Put to sleep    |             |
      | Sedative | Sleep medicine  | a_slide.jpg |
    And there is "similar to" link between note "Sedition" and "Sedation"
    And there are some notes for the current user
      | title    | description     | pictureUrl  |
      | x | y |             |

  Scenario: Different review pages for different notes
    * I do these initial reviews in sequence:
      | review_type  | title    | additional_info             |
      | single note  | Sedition | Incite violence             |
      | single note  | Sedation | Put to sleep                |
      | picture note | Sedative | Sleep medicine; a_slide.jpg |
      | link         | Sedition | similar to; Sedation     |
      | initial done |          |                             |

