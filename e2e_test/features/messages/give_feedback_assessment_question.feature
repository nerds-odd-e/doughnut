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
        And "a_trainer" can see the message "I believe the question is incorrect" when click on the question "Is 0 * 0 = 0?"
        Then "old_learner" can see the conversation with "A Trainer" for the question "Is 0 * 0 = 0?" in the message center
        And "old_learner" can see the message "I believe the question is incorrect" when click on the question "Is 0 * 0 = 0?"

    @ignore
    Scenario: User can send message to reply feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        And "a_trainer" can see the input form and Send button when click on the question "Is 0 * 0 = 0?"
        And "a_trainer" can type the message "No, it is correct" and send this message to "old_learner"
        Then "old_learner" can see the message "No, it is correct" from "a_trainer"

    Scenario: Trainer Agrees to Feedback
        Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
        Then "a_trainer" can see the button "AgreeButton" with "Old Learner" in the message center
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        Then when "a_trainer" can click the "AgreeButton" button with "Old Learner" in the message center

    @ignore
    Scenario: Trainer Declines Feedback
        Given I start the assessment from the "Just say 'Yes'" notebook in the bazaar
        When I answer the question incorrectly and submit feedback saying "I believe the question is incorrect"
        And "a_trainer" sees the "Agree" button in the message center alongside the feedback from "Old Learner"
        Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
        Then when "a_trainer" clicks the "Decline" button
        Then the system confirms rejection of the feedback with a success message
        And the "Decline" button becomes disabled to prevent further clicks
        And the feedback status is updated to "Rejected" in the assessment system