Feature: User having a conversation regarding a note
  As a user, I want to have a conversation regarding a note,
  so that trainers can improve the content and I can learn more about the topic.

  Background:
    Given I am logged in as "old_learner"
    And there is a certified notebook "Rocket Science" by "a_trainer" with 2 questions, shared to the Bazaar

  Scenario: User can start a conversation regarding a note
    Then I start a conversation about the note "Rocket Science" with a message "I believe the Earth is flat"
# Then "old_learner" should see the conversation regarding "Rocket Science" in my message center
