Feature: Message Center with Unread Message Count
    As a user, I want to see the number of unread messages in the message center.

    Background:
      Given there is a notebook with head note "Rocket Science" from user "a_trainer" shared to the Bazaar
      When "old_learner" start a conversation about the note "Rocket Science" with a message "Hi"

    Scenario: Message receiver should have 1 unread message while sender has none
      Then there should be no unread message for the user "old_learner"
      And "a_trainer" can see the notification icon with 1 unread messages

    Scenario: The message is read by the receiver
        When "a_trainer" read the conversation with "Old Learner" for the topic "Rocket Science" in the message center
        And The current page is reloaded
        Then there should be no unread message for the user "a_trainer"
