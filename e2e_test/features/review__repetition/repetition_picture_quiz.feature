Feature: Repetition Image Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Space" which skips review
    And there are some notes:
      | Topic | Parent Topic | Image Url                          | Image Mask            |
      | Earth | Space        | https://picsum.photos/id/237/20/30 | 20 40 70 30 40 80 5 8 |
      | Moon  | Space        | https://picsum.photos/id/238/20/30 | 30 40 20 30           |

  Scenario: Image question
    Given I learned one note "Earth" on day 1
    When I am repeat-reviewing my old note on day 2
    Then I should be asked image question "example.png" with options "Earth, Moon"
    And I should see the screenshot matches
