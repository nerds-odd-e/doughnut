Feature: Review Pages
  As a learner, I want to review my notes and links so that I have fresh memory.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | topic    | description     | pictureUrl  |
      | Sedition | Incite violence |             |
      | Sedation | Put to sleep    |             |
      | Sedative | Sleep medicine  | a_slide.jpg |
    And there is "similar to" link between note "Sedition" and "Sedation"

  Scenario: Different review pages for different notes
    * I do these initial reviews in sequence:
      | review_type  | topic    | additional_info             |
      | single note  | Sedition | Incite violence             |
      | single note  | Sedation | Put to sleep                |
      | picture note | Sedative | Sleep medicine; a_slide.jpg |
      | link         | Sedition | similar to; Sedation     |
      | initial done |          |                             |

  Scenario: Index page
    Given It's day 1, 8 hour
    And I do these initial reviews in sequence:
      | review_type | topic    |
      | single note | Sedition |
    When It's day 2, 9 hour
    And I go to the reviews page
    Then I should see that I have old notes to repeat
    And I should see that I have new notes to learn

