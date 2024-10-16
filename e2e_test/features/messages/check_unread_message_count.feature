Feature: Check unread message count
    As a user, I want to check the number of unread messages in the message center.

    Scenario: User has no unread messages in the message center
        Given I am logged in as "old_learner"
        Then "old_learner" can see the notification icon with no unread messages

    @ignore
    Scenario: User has 1 unread message in the message center
        And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar
        Given I am logged in as "old_learner"
        And I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        And I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the notification icon with 1 unread messages
