@usingMockedOpenAiService
Feature: Promote Point to Note
  As a learner, when I see understanding points in the assimilation page,
  I want to promote a point to become a child or sibling note,
  So that I can expand on that point as a separate note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Machine Learning" which skips review
    And there are some notes:
      | Title               | Parent Title     | Details                                                                                                                                                     |
      | Supervised Learning | Machine Learning | Supervised learning is a type of machine learning where the model is trained on labeled data to make predictions or decisions based on input-output pairs. |

  Scenario: Promote a point from understanding checklist to child note
    Given OpenAI generates understanding checklist with points:
      | Supervised learning uses labeled data |
      | It learns from input-output pairs |
      | Common algorithms include linear regression and decision trees |
    And OpenAI will extract point "It learns from input-output pairs" to child note with title "Learning from Input-Output Pairs" and details "The model learns patterns from labeled examples" and updated parent details "Supervised learning is a type of ML. See child notes for details."
    When I am assimilating the note "Supervised Learning"
    And I promote the point "It learns from input-output pairs" to a child note
    Then I should remain on the assimilation page for "Supervised Learning"
    And the point "It learns from input-output pairs" should be removed from the understanding checklist
    And the understanding checklist should still show the remaining points

  Scenario: Promote a point to sibling note
    Given OpenAI generates understanding checklist with points:
      | Supervised learning uses labeled data |
      | It learns from input-output pairs |
    And OpenAI will extract point "It learns from input-output pairs" to sibling note with title "Learning from Input-Output Pairs" and details "The model learns patterns from labeled examples" and updated parent details "Supervised learning is a type of machine learning where the model is trained on labeled data."
    When I am assimilating the note "Supervised Learning"
    And I promote the point "It learns from input-output pairs" to a sibling note
    Then I should see "Machine Learning" with these children
      | note-title                      |
      | Supervised Learning             |
      | Learning from Input-Output Pairs |
    And the point "It learns from input-output pairs" should be removed from the understanding checklist
