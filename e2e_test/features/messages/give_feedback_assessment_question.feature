Feature: Learner gives feedback on an assessment question
    As a learner, I want to provide feedback on an assessment question,
    so that trainers can improve the content and I can learn more about the topic.


    Background:
        Given I am logged in as "old_learner"
        And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar

    Scenario: There is a default "No conversation" message on the message center screen when having a feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        Then "a_trainer" can see the default message "No conversation" from message center screen
        Then "old_learner" can see the default message "No conversation" from message center screen
    
    Scenario: I can see the feedback message when click on the feedback item
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        And I can see the message "I believe the question is incorrect" when click on the question "Is 0 * 0 = 0?"
        Then "old_learner" can see the conversation with "A Trainer" for the question "Is 0 * 0 = 0?" in the message center
        And I can see the message "I believe the question is incorrect" when click on the question "Is 0 * 0 = 0?"

    @ignore
    Scenario: User can send message to reply feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        And I can see the input form and Send button when click on the question "Is 0 * 0 = 0?"
        And I can type the message "No, it is correct" and send this message to conversation room

    Scenario: Trainer Agrees to Feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the button "AgreeButton" with "Old Learner" in the message center
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        Then when "a_trainer" can click the "AgreeButton" button with "Old Learner" in the message center
        Then I should see message that says "Feedback is Accepted"
        And I should see message that says "Accepted"

    Scenario: Trainer Declines Feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the button "DeclineButton" with "Old Learner" in the message center
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        Then when "a_trainer" can click the "DeclineButton" button with "Old Learner" in the message center
        Then I should see message that says "Feedback is Rejected"
        And I should see message that says "Rejected"

    @ignore
    Scenario: Trainer Views Accepted Feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When "Old Learner" has previously submitted feedback that was accepted
        Then "a_trainer" can see the message "Accepted" in the message center
        Then "a_trainer" should not see the "AgreeButton" or "DeclineButton"

    @ignore
    Scenario: Trainer Views Rejected Feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When "Old Learner" has previously submitted feedback that was rejected
        Then "a_trainer" can see the message "Rejected" in the message center
        Then "a_trainer" should not see the "AgreeButton" or "DeclineButton"