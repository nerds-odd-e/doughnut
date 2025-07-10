Feature: semantical search
  As a learner, I want to search notes by their semantic meaning,
  so that I can find the notes that I want to review.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Physics" and details "The study of nature"
    And I have a notebook with the head note "Chemistry" and details "The study of substances"
    And there are some notes:
      | Title    | Parent Title | Details             |
      | Energy   | Physics      | The study of energy |
      | Matter   | Physics      | The study of matter |

  @mockBrowserTime
  Scenario Outline: Search at the top level
    Given I reindex the notebook "Physics"
    When I visit all my notebooks
    And I start searching
    Then I should see "<targets>" as targets only when searching "<search key>"
    Examples:
      | search key | targets     |
      | Energy     | Energy      |
      | Matter     | Matter      |
