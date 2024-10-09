Feature: Learner gives feedback on an assessment question
    As a learner, I want to provide feedback on an assessment question,
    so that trainers can improve the content and I can learn more about the topic.


    Background:
        Given I am logged in as "old_learner"
        And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar

    Scenario: I have the option to give feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the conversation "\"I believe the question is incorrect\"" with "Old Learner" in the message center
        Then "old_learner" can see the conversation "\"I believe the question is incorrect\"" with "A Trainer" in the message center
    
    @ignore
    Scenario: There is a default "No conversation" message on the message center screen when having a feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the conversation "\"I believe the question is incorrect\"" with "Old Learner" in the message center
        Then "a_trainer" can see the default message "No conversation" from message center screen
        Then "old_learner" can see the default message "No conversation" from message center screen

    Scenario: I see the button Agree and Decline
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the button "AgreeButton" with "Old Learner" in the message center
        Then "a_trainer" can see the button "DeclineButton" with "Old Learner" in the message center