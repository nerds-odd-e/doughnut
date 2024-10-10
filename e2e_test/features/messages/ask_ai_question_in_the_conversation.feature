@usingMockedOpenAiService
Feature: Ask AI question in the conversation
  User wants to ask question to AI in a conversation,
  so that they can get AI's opinion.


  # prepare data
  Background:
    Given I am logged in as "old_learner"
        And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar

  Scenario: The users can ask question to AI in the conversation
    Given I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
    When I answer the question wrongly and submit feedback saying 'I believe the question is incorrect'
    Then "a_trainer" can see the conversation with "Old Learner" for the question "Is 0 * 0 = 0?" in the message center
    And "a_trainer" can see the message "I believe the question is incorrect" when click on the question "Is 0 * 0 = 0?"
    Then I ask for AI's response
       
