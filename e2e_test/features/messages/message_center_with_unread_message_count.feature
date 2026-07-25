Feature: Message Center with Unread Message Count
    As a user, I want to see the number of unread messages in the message center.

  Scenario: Unread counts update when a conversation starts and the receiver replies
    Given there is a notebook "Trainer demos" with a note "Rocket Science" from user "a_trainer" shared to the Bazaar
    When "old_learner" start a conversation about the note "Rocket Science" with a message "Hi"
    Then I should have no unread messages
    And "a_trainer" should have 1 unread messages
    When I reply to the conversation "Rocket Science":
      | Thanks, happy to help. |
    Then "old_learner" should have 1 unread messages

  Scenario: The message is read by the receiver
    Given there is a notebook "Trainer demos" with a note "Rocket Science" from user "a_trainer" shared to the Bazaar
    When "old_learner" start a conversation about the note "Rocket Science" with a message "Hi"
    And I am re-logged in as "a_trainer"
    When I read the conversation with "Old Learner" for the subject "Rocket Science" in the message center
    Then I should have no unread messages

  Scenario: Any user in a circle read the message count as read for all circle members
    Given There is a circle "TDD Fan Club" with "a_trainer, old_learner" members and notebook "Critical Thinking" shared to the Bazaar by "a_trainer"
    When "another_old_learner" start a conversation about the note "Critical Thinking" with a message "Hi"
    Then "old_learner" should have 1 unread messages
