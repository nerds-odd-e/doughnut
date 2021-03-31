Feature: Repetition Link Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | noteContent.title | noteContent.skipReview | testingParent |
      | Space             | true       |               |
      | Moon              | true       | Space         |
      | Earth             | true       | Space         |
      | Mars              | true       | Space         |
    And there is "belongs to" link between note "Moon" and "Earth"

  Scenario Outline: "Belongs to" question
    Given It's day 1, 8 hour
    And I do these initial reviews in sequence:
      | review_type | title |
      | link        | Moon  |
    When I am repeat-reviewing my old note on day 2
    Then I should be asked link question "Moon" "belongs to" with options "Earth, Mars"
    When I choose answer "<answer>"
    Then I should see that my answer <result>

    Examples:
      | answer | result          |
      | Mars   | "Mars" is wrong |
      | Earth  | is correct      |

  Scenario: Delete link with review record
    Given I do these initial reviews in sequence:
      | review_type | title |
      | link        | Moon  |
    When I open "Space/Moon" note from top level
    Then I should be able to delete the link to note "Earth"
