Feature: Conversation about a note
  As a learner, I want to start a conversation about a note,
  so that trainers and circle members can discuss the subject.

  Scenario: Conversation about a Bazaar note appears in the owner's message center
    Given there is a notebook "Trainer demos" with a note "Rocket Science" from user "a_trainer" shared to the Bazaar
    When "old_learner" starts a conversation about the note "Rocket Science" with the message "Hi"
    Then "a_trainer" can see the conversation with "Old Learner" about "Rocket Science" in the message center:
      | message |
      | Hi      |

  Scenario: Conversation about a circle notebook is visible to both parties
    Given There is a circle "Odd-e SG Team" with "a_trainer, old_learner" members and notebook "Team agreement" by "a_trainer"
    When "old_learner" starts a conversation about the note "Team agreement" with the message "Hi"
    Then I can see the conversation with "Odd-e SG Team" about "Team agreement" in the message center:
      | message |
      | Hi      |
    And "a_trainer" can see the conversation with "Old Learner" about "Team agreement" in the message center:
      | message |
      | Hi      |
