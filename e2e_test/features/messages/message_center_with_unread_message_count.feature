Feature: Message center unread counts
  As a user, I want to see how many unread messages I have,
  so that I know when someone has messaged me about a note.

  Scenario: Starting a conversation increments the receiver's unread count
    Given there is a notebook "Trainer demos" with a note "Rocket Science" from user "a_trainer" shared to the Bazaar
    When "old_learner" starts a conversation about the note "Rocket Science" with the message "Hi"
    Then I should have no unread messages
    And "a_trainer" should have an unread message count of 1

  Scenario: Replying to a conversation increments the other party's unread count
    Given there is a notebook "Trainer demos" with a note "Rocket Science" from user "a_trainer" shared to the Bazaar
    When "old_learner" starts a conversation about the note "Rocket Science" with the message "Hi"
    And I am re-logged in as "a_trainer"
    When I reply to the conversation "Rocket Science":
      | Thanks, happy to help. |
    Then "old_learner" should have an unread message count of 1

  Scenario: Reading a conversation clears the unread count
    Given there is a notebook "Trainer demos" with a note "Rocket Science" from user "a_trainer" shared to the Bazaar
    When "old_learner" starts a conversation about the note "Rocket Science" with the message "Hi"
    And I am re-logged in as "a_trainer"
    When I read the conversation with "Old Learner" about "Rocket Science"
    Then I should have no unread messages

  Scenario: Circle members get an unread count for conversations about a shared notebook
    Given There is a circle "TDD Fan Club" with "a_trainer, old_learner" members and notebook "Critical Thinking" shared to the Bazaar by "a_trainer"
    When "another_old_learner" starts a conversation about the note "Critical Thinking" with the message "Hi"
    Then "old_learner" should have an unread message count of 1
