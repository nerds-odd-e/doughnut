Feature: Check unread message count
    As a user, I want to check the number of unread messages in the message center.


    Background:
        Given I am logged in as "old_learner"
        And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar
        And I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        And I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'

    Scenario: User has no unread messages in the message center
        Then "old_learner" can see the notification icon with no unread messages

    Scenario: User has 1 unread message in the message center    
        Then "a_trainer" can see the notification icon with 1 unread messages

    Scenario: User has 1 unread message, after clicking the notification icon, the unread message count is 0
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        And I can see the message "I believe the question is incorrect" in the conversation "Is 0 * 0 = 0?"
        And The current page is reloaded
        Then "a_trainer" can see the notification icon with no unread messages
        Then "old_learner" can see the conversation with "A Trainer" for the question "Is 0 * 0 = 0?" in the message center
        And I can see the message "I believe the question is incorrect" in the conversation "Is 0 * 0 = 0?"
        And The current page is reloaded
        Then "old_learner" can see the notification icon with no unread messages
