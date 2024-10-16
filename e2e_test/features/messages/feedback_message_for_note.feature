@ignore
Feature: User having a conversation regarding a note
  As a user, I want to have a conversation regarding a note,
  so that trainers can improve the content and I can learn more about the topic.

  Background:
    Given I am logged in as "old_learner"
    And I have a notebook with the head note "Rocket Science"

  Scenario: User can start a conversation regarding a note
    When I start a conversation about the note "Rocket Science" with a message "I believe the Earth is flat"
    Then I should see the conversation regarding "Rocket Science" in my message center
