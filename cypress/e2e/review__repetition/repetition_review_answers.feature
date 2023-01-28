Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description                    | skipReview | testingParent |
      | English  |                                | true       |               |
      | sedition | Sedition means incite violence | false      | English       |
      | sedation | Put to sleep is sedation       | false      | English       |
      | medical  |                                | true       | English       |

  Scenario: View last result when the quiz answer was correct
    Given I learned one note "sedition" on day 1
    And I learned one note "sedation" on day 1
    When I am repeat-reviewing my old note on day 2
    And I choose answer "sedition"
    Then I view the last result
    And I should see that my answer is correct
    Then I should see "sedition" in note title
    And I should see the info of note "sedition"
      | Repetition Count |
      | 1                |

  @mockBrowserTime
  Scenario: I can remove a note from further reviews
    Given I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    And I choose answer "sedition"
    Then I view the last result
    When choose to remove it from reviews
    Then I should see the review point is removed from review
    And On day 100 I should have "1/1" note for initial review and "0/0" for repeat
