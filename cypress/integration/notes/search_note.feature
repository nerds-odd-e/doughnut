Feature: search note
  As a learner, I want to maintain my newly acquired knowledge in
  notes that linking to each other, so that I can review them in the
  future.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description     |
      | Sedation | Put to sleep    |
      | Sedative | Sleep medicine  |

  @stopTime
  Scenario Outline: Search at the top level
    Given I visit all my notebooks
    Then I start searching
    Then I should see "<targets>" as targets only when searching "<search key>"
    Examples:
      | search key | targets            |
      | Sed        | Sedation, Sedative |
      | Sedatio    | Sedation           |
