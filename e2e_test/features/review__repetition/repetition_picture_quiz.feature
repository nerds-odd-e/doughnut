Feature: Repetition Picture Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | skipReview | testingParent | pictureUrl                         | pictureMask           |
      | Space            | true       |               |                                    |                       |
      | Earth            |            | Space         | https://picsum.photos/id/237/20/30 | 20 40 70 30 40 80 5 8 |
      | Moon             |            | Space         | https://picsum.photos/id/238/20/30 | 30 40 20 30           |
      | Mars             | true       | Space         |                                    |                       |

  Scenario: Picture question
    Given I learned one note "Earth" on day 1
    When I am repeat-reviewing my old note on day 2
    Then I should be asked picture question "example.png" with options "Earth, Moon"
    And I should see the screenshot matches

  Scenario: Picture selection question
    Given The randomizer always choose the last
    Given I learned one note "Earth" on day 1
    When I am repeat-reviewing my old note on day 2
    Then I should be asked picture selection question "Earth" with "example.png, moon.jpg"
    And I should see the screenshot matches

