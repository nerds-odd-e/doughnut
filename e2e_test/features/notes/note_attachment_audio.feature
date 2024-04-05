Feature: Attach and download audio file
  As a learner, I want to attach audio file to my notes so that I can review them in the future.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent | details      |
      | LeSS in Action   |               | I'm testing. |
    When I create a notebook with "LeSS in Action" topic

  Scenario: Attach audio file successful
    When I attach audio file "spring.mp3" to my note
    And I submit successful
    # And I should see "spring.mp3" file in my note
    # Then Then I can download that audio file in my note 



