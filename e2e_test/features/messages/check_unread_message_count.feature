Feature: Check unread message count
    As a user, I want to check the number of unread messages in the message center.

    Background:
        Given I am logged in as an existing user
        And I have one unread message in the message center

    Scenario: User has one unread message in the message center
        Then I can see a badge with the number "1" on the notification icon in the topbar.

    Scenario: User has no unread messages in the message center
        Then I can see the notification icon in the topbar without any accompanying badge.
