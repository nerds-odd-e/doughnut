Feature: User having a conversation regarding a note
  As a user, I want to have a conversation regarding a note,
  so that trainers can improve the content and I can learn more about the topic.

  Scenario: Users can see the conversation regarding a note in a bazaar
    Given I am logged in as "old_learner"
    And I have a notebook with the head note "Rocket Science"
    When I start a conversation about the note "Rocket Science" with a message "Hi"
    Then "a_trainer" can see the conversation with "Old Learner" for the note "Rocket Science" in the message center
    And I can see the message "Hi" in the conversation with "Old Learner"
    And "old_learner" can see the conversation with "A Trainer" for the note "Rocket Science" in the message center
    And I can see the message "Hi" in the conversation with "A Trainer"

  Scenario: Users can see the conversation regarding a note in a circle
    Given There is a circle "Odd-e SG Team" with "a_trainer, old_learner, another_old_learner" members
    And I am logged in as "a_trainer"
    And There is a notebook "Team agreement" in circle "Odd-e SG Team"

    When I am re-logged in as "old_learner"
    And I start a conversation about the note "Team agreement" with a message "Hi"

    Then all circle members "a_trainer, old_learner, another_old_learner" can view the message "Hi" in conversation with "Odd-e SG Team" for the note "Team agreement" in the message center
