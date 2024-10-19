Feature: User having a conversation regarding a note
  As a user, I want to have a conversation regarding a note,
  so that trainers can improve the content and I can learn more about the topic.

  Scenario: User send message about a note shared to a bazaar
    Given there is a notebook with head note "Rocket Science" from user "a_trainer" shared to the Bazaar
    When "old_learner" start a conversation about the note "Rocket Science" with a message "Hi"
    Then "a_trainer" can see the conversation with "Old Learner" for the topic "Rocket Science" in the message center:
      | message |
      | Hi      |
    And "old_learner" can see the conversation with "A Trainer" for the topic "Rocket Science" in the message center:
      | message |
      | Hi      |

  Scenario: User send message about a note in a circle
    Given There is a circle "Odd-e SG Team" with "a_trainer, old_learner, another_old_learner" members
    And There is a notebook "Team agreement" in circle "Odd-e SG Team" by "a_trainer"
    When "old_learner" start a conversation about the note "Team agreement" with a message "Hi"
    Then all circle members "a_trainer, old_learner, another_old_learner" can view the conversation with "Odd-e SG Team" for the note "Team agreement" in the message center
