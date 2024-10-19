Feature: User having a conversation regarding a note
  As a user, I want to have a conversation regarding a note,
  so that trainers can improve the content and I can learn more about the topic.

  Scenario: Users can see the conversation regarding a note in a bazaar
    Given there is a notebook with head note "Rocket Science" from user "a_trainer" shared to the Bazaar
    When I am logged in as "old_learner"
    And I start a conversation about the note "Rocket Science" with a message "Hi"
    Then "a_trainer" can see the conversation with "Old Learner" for the topic "Rocket Science" in the message center:
      | message |
      | Hi      |
    And "old_learner" can see the conversation with "A Trainer" for the topic "Rocket Science" in the message center:
      | message |
      | Hi      |

  Scenario: Users can see the conversation regarding a note in a circle
    Given There is a circle "Odd-e SG Team" with "a_trainer, old_learner, another_old_learner" members
    And I am logged in as "a_trainer"
    And There is a notebook "Team agreement" in circle "Odd-e SG Team"

    When I am re-logged in as "old_learner"
    And I start a conversation about the note "Team agreement" with a message "Hi"

    Then all circle members "a_trainer, old_learner, another_old_learner" can view the message "Hi" in conversation with "Odd-e SG Team" for the note "Team agreement" in the message center
