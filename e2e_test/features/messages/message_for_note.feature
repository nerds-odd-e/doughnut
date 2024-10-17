Feature: User having a conversation regarding a note
  As a user, I want to have a conversation regarding a note,
  so that trainers can improve the content and I can learn more about the topic.

  Scenario: Users can see the conversation regarding a note in a bazaar
    Given I am logged in as "old_learner"
    And there is a certified notebook "Rocket Science" by "a_trainer" with 2 questions, shared to the Bazaar
    When I start a conversation about the note "Rocket Science" with a message "I believe the Earth is flat"
    Then "a_trainer" can see the conversation with "Old Learner" in the message center
    And I can see the message "I believe the Earth is flat" in the conversation with "Old Learner"
    And "old_learner" can see the conversation with "A Trainer" in the message center
    And I can see the message "I believe the Earth is flat" in the conversation with "A Trainer"

  Scenario: Users can see the conversation regarding a note in a circle
    Given There is a circle "Odd-e SG Team" with "a_trainer, old_learner, another_old_learner" members
    And I am logged in as "a_trainer"
    And I create a notebook "Team agreement" in circle "Odd-e SG Team"

    When I am re-logged in as "old_learner"
    And There is a notebook "Team agreement" in circle "Odd-e SG Team"
    And I start a conversation about the note "Team agreement" with a message "I believe the Earth is flat"

    Then "old_learner" can see the conversation with "Odd-e SG Team" in the message center
    And I can see the message "I believe the Earth is flat" in the conversation with "Odd-e SG Team"
    And "a_trainer" can see the conversation with "Odd-e SG Team" in the message center
    And I can see the message "I believe the Earth is flat" in the conversation with "Odd-e SG Team"
    And "another_old_learner" can see the conversation with "Odd-e SG Team" in the message center
    And I can see the message "I believe the Earth is flat" in the conversation with "Odd-e SG Team"
