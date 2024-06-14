Feature: Approve Quiz Question
  As an owner, I want to approve the quiz questions for the notes of a notebook,
  so that I can use these questions for assessment.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "Animal joking"
    And there are questions for the note:
      | noteTopic     | question             | answer | oneWrongChoice |
      | Animal joking | What does a cow say? | moo    | woo            |
      | Animal joking | What does a dog say? | quo    | huo            |
      | Animal joking | What does a cat say? | meo    | huo            |

  Scenario Outline: Approve quiz question
    When I change approval status of the question "<question>" of the topic "<note>" to "<approval>":
    Then I see the "<question>" question of the "<note>" note has "<approval>"
    Examples:
      | note          | question             | approval  |
      | Animal joking | What does a cow say? | approve   |
      | Animal joking | What does a dog say? | approve   |
      | Animal joking | What does a cat say? | unapprove |
