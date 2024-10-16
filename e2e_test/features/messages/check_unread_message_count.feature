Feature: Check unread message count
    As a user, I want to check the number of unread messages in the message center.

    Scenario: User has no unread messages in the message center
        Given I am logged in as "old_learner"
        Then "old_learner" can see the notification icon with no unread messages
