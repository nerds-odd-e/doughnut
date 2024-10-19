Feature: Learner gives feedback on an assessment question
    As a learner, I want to provide feedback on an assessment question,
    so that trainers can improve the content and I can learn more about the topic.

  Background:
    Given I am logged in as "old_learner"
    And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar
    And I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
    And I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'

  Scenario: Both sender and receiver can see the feedback message
    Then "a_trainer" can see the conversation with "Old Learner" for the topic "Is 0 * 0 = 0?" in the message center:
      | message                             |
      | I believe the question is incorrect |
    Then "old_learner" can see the conversation with "A Trainer" for the topic "Is 0 * 0 = 0?" in the message center:
      | message                             |
      | I believe the question is incorrect |

  Scenario: User can reply to the feedback message
    Given I am re-logged in as "a_trainer"
    When I reply "No, it is correct" to the conversation "Is 0 * 0 = 0?"
    Then I should see the new message "No, it is correct" on the current user's side of the conversation
    And I should see the new message "I believe the question is incorrect" on the other user's side of the conversation
